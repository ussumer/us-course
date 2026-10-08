package com.us.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习计划进度信息（GET /lessons/plans 返回的 list 元素）
 * <p>课前资料只提供了原型截图未提供源码，字段按 day02 笔记 1.2.4 的接口规格定义</p>
 */
@Data
@ApiModel(description = "学习计划进度信息")
public class LearningPlanVO {

    @ApiModelProperty("课表id")
    private Long id;

    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("本周学习的小节数量")
    private Integer weekLearnedSections;

    @ApiModelProperty("本周计划学习数量")
    private Integer weekFreq;

    @ApiModelProperty("总已学习小节数量")
    private Integer learnedSections;

    @ApiModelProperty("总小节数量")
    private Integer sections;

    @ApiModelProperty("最近一次学习时间")
    private LocalDateTime latestLearnTime;
}
