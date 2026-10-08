package com.us.learning.mq;

import com.us.api.dto.exam.ExamFinishedDTO;
import com.us.common.constants.MqConstants;
import com.us.common.utils.UserContext;
import com.us.learning.domain.dto.LearningRecordFormDTO;
import com.us.learning.domain.po.LearningLesson;
import com.us.learning.enums.SectionType;
import com.us.learning.service.ILearningLessonService;
import com.us.learning.service.ILearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 考试完成消息监听器（day13~14：考试小节属于课程学习的一部分，需要新增学习记录）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamFinishedListener {

    private final ILearningLessonService lessonService;

    private final ILearningRecordService recordService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "exam.learned.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.EXAM_FINISHED
    ))
    public void listenExamFinishedMessage(ExamFinishedDTO dto) {
        // 1.健壮性处理
        if (dto == null || dto.getUserId() == null || dto.getCourseId() == null || dto.getSectionId() == null) {
            log.error("接收到考试完成MQ消息有误，消息体为空");
            return;
        }
        try {
            // 2.MQ线程没有登录信息，手动放入用户上下文（addLearningRecord内部依赖UserContext）
            UserContext.setUser(dto.getUserId());
            // 3.查询课表
            LearningLesson lesson = lessonService.queryByUserAndCourseId(dto.getUserId(), dto.getCourseId());
            if (lesson == null) {
                log.error("考试完成通知：用户{}未报名课程{}，无法记录学习进度", dto.getUserId(), dto.getCourseId());
                return;
            }
            // 4.提交学习记录（考试小节，直接标记为已完成）
            LearningRecordFormDTO form = new LearningRecordFormDTO();
            form.setSectionType(SectionType.EXAM);
            form.setLessonId(lesson.getId());
            form.setSectionId(dto.getSectionId());
            form.setMoment(0);
            form.setDuration(0);
            form.setCommitTime(dto.getFinishTime());
            recordService.addLearningRecord(form);
        } finally {
            // 5.清理ThreadLocal，避免线程复用时的脏数据
            UserContext.removeUser();
        }
    }
}
