package com.us.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.api.client.course.CourseClient;
import com.us.api.dto.course.CourseFullInfoDTO;
import com.us.api.dto.leanring.LearningLessonDTO;
import com.us.api.dto.leanring.LearningRecordDTO;
import com.us.common.autoconfigure.mq.RabbitMqHelper;
import com.us.common.constants.MqConstants;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.exceptions.DbException;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.UserContext;
import com.us.learning.domain.dto.LearningRecordFormDTO;
import com.us.learning.domain.po.LearningLesson;
import com.us.learning.domain.po.LearningRecord;
import com.us.learning.enums.LessonStatus;
import com.us.learning.enums.SectionType;
import com.us.learning.mapper.LearningRecordMapper;
import com.us.learning.service.ILearningLessonService;
import com.us.learning.service.ILearningRecordService;
import com.us.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>
 * 学习记录表 服务实现类（day04 改造：高频进度更新走 Redis 合并写和持久化到期索引）
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {

    private final ILearningLessonService lessonService;

    private final CourseClient courseClient;

    private final LearningRecordDelayTaskHandler taskHandler;

    private final RabbitMqHelper mqHelper;

    /**
     * 查询当前用户指定课程的学习进度（Feign接口）
     */
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.查询课表（增强：不存在或过期直接返回空，防止NPE）
        LearningLesson lesson = lessonService.queryByUserAndCourseId(userId, courseId);
        if (lesson == null || lesson.getStatus() == LessonStatus.EXPIRED) {
            return null;
        }
        // 3.查询学习记录
        // select * from xx where lesson_id = #{lessonId}
        List<LearningRecord> records = lambdaQuery().eq(LearningRecord::getLessonId, lesson.getId()).list();
        // 4.封装结果
        LearningLessonDTO dto = new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
        dto.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
        return dto;
    }

    /**
     * 提交学习记录（day04 改造后：只有"第一次学完"才走数据库和课表更新，
     * 普通进度更新全部进入 Redis 缓存 + 延迟任务异步持久化）
     */
    @Override
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO recordDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        LearningLesson ownedLesson = lessonService.getById(recordDTO.getLessonId());
        if (ownedLesson == null || !Objects.equals(userId, ownedLesson.getUserId())
                || ownedLesson.getStatus() == LessonStatus.EXPIRED
                || (ownedLesson.getExpireTime() != null && ownedLesson.getExpireTime().isBefore(LocalDateTime.now()))) {
            throw new BizIllegalException("无权提交该课程的学习记录");
        }
        if (recordDTO.getCommitTime() == null) recordDTO.setCommitTime(LocalDateTime.now());
        if (recordDTO.getSectionType() == SectionType.VIDEO &&
                (recordDTO.getMoment() == null || recordDTO.getMoment() < 0
                 || recordDTO.getDuration() == null || recordDTO.getDuration() <= 0
                 || recordDTO.getMoment() > recordDTO.getDuration())) {
            throw new BizIllegalException("视频时长或进度不合法");
        }
        // 2.处理学习记录
        boolean finished = false;
        if (recordDTO.getSectionType() == SectionType.VIDEO) {
            // 2.1.处理视频
            finished = handleVideoRecord(userId, recordDTO);
        } else {
            // 2.2.处理考试
            finished = handleExamRecord(userId, recordDTO);
        }
        if (!finished) {
            // 没有新学完的小节，无需更新课表中的学习进度
            // （播放进度的持久化、最近学习小节/时间的更新都由延迟任务异步完成）
            return;
        }
        // 3.处理课表数据
        handleLearningLessonsChanges(recordDTO);
    }

    private void handleLearningLessonsChanges(LearningRecordFormDTO recordDTO) {
        // 1.查询课表
        LearningLesson lesson = lessonService.getById(recordDTO.getLessonId());
        if (lesson == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        // 2.判断是否有新的完成小节
        boolean allLearned = false;

        // 3.如果有新完成的小节，则需要查询课程数据
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BizIllegalException("课程不存在，无法更新数据！");
        }
        int sections = cInfo.getSectionNum();
        if (sections <= 0) throw new BizIllegalException("课程没有可学习的小节");
        // 在数据库行锁内按当前值判断，两个不同小节同时完成也不会覆盖最终状态。
        lessonService.lambdaUpdate()
                .setSql("status = CASE WHEN learned_sections + 1 >= " + sections + " THEN 2 ELSE 1 END")
                .setSql("finish_time = CASE WHEN learned_sections + 1 >= " + sections + " THEN NOW() ELSE finish_time END")
                .setSql("learned_sections = learned_sections + 1")
                .set(LearningLesson::getLatestSectionId, recordDTO.getSectionId())
                .set(LearningLesson::getLatestLearnTime, LocalDateTime.now())
                .eq(LearningLesson::getId, lesson.getId()).update();
        // 6.发送MQ消息，通知积分系统（day07 练习4.2：学完一小节得10分，每日上限50）
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.LEARN_SECTION, lesson.getUserId());
    }

    private boolean handleVideoRecord(Long userId, LearningRecordFormDTO recordDTO) {
        // 1.查询旧的学习记录（优先走缓存，未命中再查库并回填缓存）
        LearningRecord old = queryOldRecord(recordDTO.getLessonId(), recordDTO.getSectionId());
        if (old == null) {
            LearningRecord record = BeanUtils.copyBean(recordDTO, LearningRecord.class);
            record.setId(IdWorker.getId());
            record.setUserId(userId);
            boolean complete = (long) recordDTO.getMoment() * 2 >= recordDTO.getDuration();
            record.setFinished(complete);
            record.setFinishTime(complete ? recordDTO.getCommitTime() : null);
            if (baseMapper.insertIfAbsent(record) == 1) {
                return complete;
            }
            old = lambdaQuery().eq(LearningRecord::getLessonId, recordDTO.getLessonId())
                    .eq(LearningRecord::getSectionId, recordDTO.getSectionId()).one();
        }
        // 4.存在，则更新
        // 4.1.判断是否是第一次完成：之前未学完 且 本次播放进度超过50%
        boolean finished = !Boolean.TRUE.equals(old.getFinished()) && (long) recordDTO.getMoment() * 2 >= recordDTO.getDuration();
        if (!finished) {
            if (Boolean.TRUE.equals(old.getFinished())) return false;
            // 高频情况（约95%）：只是更新播放进度 → 不写库，写Redis缓存，由延迟任务异步持久化
            LearningRecord record = new LearningRecord();
            record.setLessonId(recordDTO.getLessonId());
            record.setSectionId(recordDTO.getSectionId());
            record.setMoment(recordDTO.getMoment());
            record.setId(old.getId());
            record.setFinished(old.getFinished());
            taskHandler.addLearningRecordTask(record);
            return false;
        }
        // 4.2.是第一次学完：学完状态必须立即生效，直接写数据库
        boolean success = lambdaUpdate()
                .set(LearningRecord::getMoment, recordDTO.getMoment())
                .set(LearningRecord::getFinished, true)
                .set(LearningRecord::getFinishTime, recordDTO.getCommitTime())
                .eq(LearningRecord::getId, old.getId())
                .eq(LearningRecord::getFinished, false)
                .update();
        if (!success) {
            return false; // 另一并发请求已完成，不能重复累计课表进度。
        }
        // 4.3.清理缓存：缓存中的finished=false已与数据库不一致，清掉等下次回源重建
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                taskHandler.cleanRecordCache(recordDTO.getLessonId(), recordDTO.getSectionId());
            }
        });
        return true;
    }

    private LearningRecord queryOldRecord(Long lessonId, Long sectionId) {
        // 1.查询缓存
        LearningRecord record = taskHandler.readRecordCache(lessonId, sectionId);
        // 2.如果命中，直接返回
        if (record != null) {
            return record;
        }
        // 3.未命中，查询数据库
        record = lambdaQuery()
                .eq(LearningRecord::getLessonId, lessonId)
                .eq(LearningRecord::getSectionId, sectionId)
                .one();
        // 4.回填缓存（增强：笔记原版在record为null时也会writeRecordCache导致NPE，虽被try-catch兜住，这里显式判空更清晰）
        if (record != null) {
            taskHandler.writeRecordCache(record);
        }
        return record;
    }

    private boolean handleExamRecord(Long userId, LearningRecordFormDTO recordDTO) {
        LearningRecord record = BeanUtils.copyBean(recordDTO, LearningRecord.class);
        record.setId(IdWorker.getId());
        record.setUserId(userId);
        record.setMoment(0);
        record.setFinished(true);
        record.setFinishTime(recordDTO.getCommitTime());
        return baseMapper.insertIfAbsent(record) == 1;
    }
}
