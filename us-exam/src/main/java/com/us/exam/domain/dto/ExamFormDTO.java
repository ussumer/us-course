package com.us.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 开始考试的表单实体（day13~14 笔记 3.2.1）
 */
@Data
@ApiModel(description = "开始考试表单")
public class ExamFormDTO {

    @ApiModelProperty("课程id")
    @NotNull(message = "课程id不能为空")
    private Long courseId;

    @ApiModelProperty("小节id")
    @NotNull(message = "小节id不能为空")
    private Long sectionId;

    @ApiModelProperty("考试类型，1：练习，2：考试")
    @NotNull(message = "考试类型不能为空")
    private Integer type;
}
