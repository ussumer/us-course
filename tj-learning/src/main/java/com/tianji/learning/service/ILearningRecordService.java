package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.po.LearningRecord;

/**
 * <p>
 * 学习记录表 服务类
 * </p>
 */
public interface ILearningRecordService extends IService<LearningRecord> {

    /**
     * 查询当前用户指定课程的学习进度（Feign接口，课程微服务拼装课程学习页数据用）
     * @param courseId 课程id
     * @return 课表信息 + 该课程全部小节的学习记录
     */
    LearningLessonDTO queryLearningRecordByCourse(Long courseId);

    /**
     * 提交学习记录（视频播放进度心跳 / 考试完成通知）
     * @param formDTO 学习记录表单
     */
    void addLearningRecord(LearningRecordFormDTO formDTO);
}
