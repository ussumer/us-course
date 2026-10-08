package com.us.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 考试详情中的单题作答信息（day13~14 笔记 3.2.4）
 */
@Data
@ApiModel(description = "考试详情-单题作答信息")
public class ExamRecordDetailVO {

    @ApiModelProperty("学员答案")
    private String answer;

    @ApiModelProperty("老师评语")
    private String comment;

    @ApiModelProperty("是否正确")
    private Boolean correct;

    @ApiModelProperty("学员得分")
    private Integer score;

    @ApiModelProperty("题目信息（含正确答案与解析）")
    private ExamQuestionVO question;
}
