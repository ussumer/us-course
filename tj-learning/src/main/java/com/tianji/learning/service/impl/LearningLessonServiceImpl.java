package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 */
@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {

    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;

    /**
     * 添加课程到课表（MQ：order.pay，用户支付/报名课程后触发）
     */
    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        // 1.查询课程有效期
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cInfoList)) {
            // 课程不存在，无法添加
            log.error("课程信息不存在，无法添加到课表");
            return;
        }
        // 2.循环遍历，处理LearningLesson数据
        List<LearningLesson> list = new ArrayList<>(cInfoList.size());
        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            LearningLesson lesson = new LearningLesson();
            // 2.1.获取过期时间 = 当前时间 + 课程有效期（单位：月）
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration != null && validDuration > 0) {
                LocalDateTime now = LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            // 2.2.填充userId和courseId，其余字段取默认值（未学习、无计划、已学0节）
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);
        }
        // 3.过滤掉已报名的课程（MQ消息可能重复投递，表对(user_id, course_id)有唯一索引）
        Set<Long> existedCourseIds = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds)
                .list()
                .stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        list = list.stream()
                .filter(l -> !existedCourseIds.contains(l.getCourseId()))
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(list)) {
            return;
        }
        // 4.批量新增
        saveBatch(list);
    }

    /**
     * 从课表删除课程（MQ：order.refund，退款成功后触发）
     */
    @Override
    @Transactional
    public void deleteUserLessons(Long userId, List<Long> courseIds) {
        // 1.健壮性处理
        if (userId == null || CollUtils.isEmpty(courseIds)) {
            return;
        }
        // 2.删除课表中该用户对应课程的记录
        remove(Wrappers.<LearningLesson>lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds));
    }

    /**
     * 分页查询我的课表
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.分页查询课表，默认按最近学习时间倒序
        // select * from learning_lesson where user_id = #{userId} order by latest_learn_time desc limit ...
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId) // where user_id = #{userId}
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.远程查询课程信息（course-service）
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        // 4.封装VO返回
        List<LearningLessonVO> list = new ArrayList<>(records.size());
        for (LearningLesson r : records) {
            // 4.1.拷贝基础属性到vo（id、courseId、status、learnedSections、createTime、expireTime、planStatus、weekFreq）
            LearningLessonVO vo = BeanUtils.copyBean(r, LearningLessonVO.class);
            // 4.2.填充课程名称、封面、总小节数
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCourseCoverUrl(cInfo.getCoverUrl());
                vo.setSections(cInfo.getSectionNum());
            }
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }

    private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        // 3.1.提取课程id
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        // 3.2.远程查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList)) {
            throw new BadRequestException("课程信息不存在！");
        }
        // 3.3.转成map，key是courseId，方便逐条查找
        return cInfoList.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
    }

    /**
     * 查询我正在学习的课程（课表中status=1且最近学习的那一条）
     */
    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询正在学习的课程
        // select * from learning_lesson where user_id = #{userId} and status = 1 order by latest_learn_time desc limit 1
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        // 3.拷贝基础属性到VO
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        // 4.查询课程信息（远程调用course-service）
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        // 5.统计课表中的课程总数
        // select count(1) from learning_lesson where user_id = #{userId}
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount);
        // 6.查询最近学习的小节信息（从未学习过时latestSectionId为null，无需查询）
        if (lesson.getLatestSectionId() != null) {
            List<CataSimpleInfoDTO> cataInfos =
                    catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
            if (!CollUtils.isEmpty(cataInfos)) {
                CataSimpleInfoDTO cataInfo = cataInfos.get(0);
                vo.setLatestSectionName(cataInfo.getName());
                vo.setLatestSectionIndex(cataInfo.getCIndex());
            }
        }
        return vo;
    }

    /**
     * 查询用户课表中指定课程的学习状态（课程详情页判断"已购/未购"）
     */
    @Override
    public LearningLessonVO queryLessonStatus(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询课表中该课程的记录，没有则返回null（前端展示"立即购买"）
        LearningLesson lesson = getTargetLesson(userId, courseId);
        // 3.拷贝为VO（含id、courseId、status、learnedSections、createTime、expireTime、planStatus）
        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }

    /**
     * 删除课表中已失效的课程（用户手动删除）
     */
    @Override
    public void deleteCourse(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询课表中该课程的记录
        LearningLesson lesson = getTargetLesson(userId, courseId);
        // 3.校验：只能删除已失效的课程
        if (lesson == null) {
            throw new BadRequestException("课程不在你的课表中");
        }
        if (lesson.getStatus() != LessonStatus.EXPIRED) {
            throw new BadRequestException("只有已失效的课程才能删除");
        }
        // 4.删除
        removeById(lesson.getId());
    }

    /**
     * 校验当前用户是否报名了课程且课程有效（Feign内部接口，媒资服务播放视频前校验）
     */
    @Override
    public Long isLessonValid(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询课表中该课程的记录
        LearningLesson lesson = getTargetLesson(userId, courseId);
        if (lesson == null) {
            return null;
        }
        // 3.校验课程是否有效：状态不是已过期，且未超过有效期时间
        boolean valid = lesson.getStatus() != LessonStatus.EXPIRED
                && (lesson.getExpireTime() == null || lesson.getExpireTime().isAfter(LocalDateTime.now()));
        return valid ? lesson.getId() : null;
    }

    /**
     * 统计课程的学习人数（Feign内部接口，排除已失效记录）
     */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        // select count(1) from learning_lesson where course_id = #{courseId} and status != 3
        return lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED.getValue())
                .count();
    }

    private LearningLesson getTargetLesson(Long userId, Long courseId) {
        return lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
    }
}
