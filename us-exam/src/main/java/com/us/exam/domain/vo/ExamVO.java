package com.us.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 开始考试返回结果（day13~14 笔记 3.2.1）
 */
@Data
@ApiModel(description = "考试信息")
public class ExamVO {

    @ApiModelProperty("考试记录id，交卷时使用")
    private Long id;

    @ApiModelProperty("考试题目列表")
    private List<ExamQuestionVO> questions;
}
