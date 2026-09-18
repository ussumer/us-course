package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 */
@Api(tags = "我的课表相关接口")
@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService lessonService;

    /**
     * 分页查询我的课表
     * 排序方式：latest_learn_time最近学习时间排序（默认），create_time购买时间排序
     */
    @ApiOperation("分页查询我的课表")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return lessonService.queryMyLessons(query);
    }

    /**
     * 查询我最近正在学习的课程（个人中心、首页悬浮窗）
     */
    @ApiOperation("查询我正在学习的课程")
    @GetMapping("/now")
    public LearningLessonVO queryMyCurrentLesson() {
        return lessonService.queryMyCurrentLesson();
    }

    /**
     * 根据课程id查询当前用户课表中该课程的学习状态（课程详情页展示已购/未购）
     */
    @ApiOperation("查询当前用户课表中指定课程的学习状态")
    @GetMapping("/{courseId}")
    public LearningLessonVO queryLessonStatus(@PathVariable("courseId") Long courseId) {
        return lessonService.queryLessonStatus(courseId);
    }

    /**
     * 删除课表中已失效的课程
     */
    @ApiOperation("删除课表中指定的失效课程")
    @DeleteMapping("/{courseId}")
    public void deleteCourse(@PathVariable("courseId") Long courseId) {
        lessonService.deleteCourse(courseId);
    }

    /**
     * 校验当前用户是否报名了课程且课程有效（Feign内部接口，媒资服务播放视频前校验）
     * @return lessonId，已报名且有效则返回lessonId，否则返回null
     */
    @ApiOperation("校验当前用户是否可以学习当前课程")
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId) {
        return lessonService.isLessonValid(courseId);
    }

    /**
     * 统计课程学习人数（Feign内部接口，课程微服务统计报名人数用）
     */
    @ApiOperation("统计课程学习人数")
    @GetMapping("/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId) {
        return lessonService.countLearningLessonByCourse(courseId);
    }
}
