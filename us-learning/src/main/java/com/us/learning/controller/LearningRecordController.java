package com.us.learning.controller;

import com.us.api.dto.leanring.LearningLessonDTO;
import com.us.learning.domain.dto.LearningRecordFormDTO;
import com.us.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 */
@RestController
@RequestMapping("/learning-records")
@Api(tags = "学习记录的相关接口")
@RequiredArgsConstructor
public class LearningRecordController {

    private final ILearningRecordService recordService;

    /**
     * 查询指定课程的学习记录（Feign接口，课程微服务拼装学习页数据用）
     */
    @ApiOperation("查询指定课程的学习记录")
    @GetMapping("/course/{courseId}")
    public LearningLessonDTO queryLearningRecordByCourse(
            @ApiParam(value = "课程id", example = "2") @PathVariable("courseId") Long courseId) {
        return recordService.queryLearningRecordByCourse(courseId);
    }

    /**
     * 提交学习记录（视频播放中每15秒心跳提交一次；考试结束时提交一次）
     */
    @ApiOperation("提交学习记录")
    @PostMapping
    public void addLearningRecord(@RequestBody @Valid LearningRecordFormDTO formDTO) {
        recordService.addLearningRecord(formDTO);
    }
}
