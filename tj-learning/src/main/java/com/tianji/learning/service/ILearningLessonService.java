package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * 添加课程到课表（MQ：order.pay，用户支付/报名课程后触发）
     * @param userId 学员id
     * @param courseIds 课程id集合
     */
    void addUserLessons(Long userId, List<Long> courseIds);

    /**
     * 从课表删除课程（MQ：order.refund，退款成功后触发）
     * @param userId 学员id
     * @param courseIds 课程id集合
     */
    void deleteUserLessons(Long userId, List<Long> courseIds);

    /**
     * 分页查询我的课表
     * @param query 分页、排序参数
     * @return 课表分页结果
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    /**
     * 查询我最近正在学习的课程（首页悬浮窗、我的课程页）
     * @return 课表信息，没有正在学习的课程时返回null
     */
    LearningLessonVO queryMyCurrentLesson();

    /**
     * 根据课程id查询当前用户课表中该课程的学习状态（课程详情页）
     * @param courseId 课程id
     * @return 课表信息，未报名返回null
     */
    LearningLessonVO queryLessonStatus(Long courseId);

    /**
     * 删除课表中已失效的课程（用户手动删除）
     * @param courseId 课程id
     */
    void deleteCourse(Long courseId);

    /**
     * 校验当前用户是否报名了课程且课程有效（Feign内部接口）
     * @param courseId 课程id
     * @return lessonId，已报名且有效则返回lessonId，否则返回null
     */
    Long isLessonValid(Long courseId);

    /**
     * 统计课程学习人数（Feign内部接口）
     * @param courseId 课程id
     * @return 学习人数
     */
    Integer countLearningLessonByCourse(Long courseId);
}
