package com.tianji.learning.task;

import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LearningLessonTask {
    private final ILearningLessonService service;

    @Scheduled(cron = "0 0 4 * * ?")
    public void checkLessonExpired() {
        service.lambdaUpdate()
                .lt(LearningLesson::getExpireTime, LocalDateTime.now())
                .ne(
                        LearningLesson::getStatus,
                        LessonStatus.EXPIRED
                )
                .set(LearningLesson::getStatus, LessonStatus.EXPIRED)
                .update();
    }
}
