package com.us.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 考试答题信息（day13~14 笔记 3.2.2）
 */
@Data
@ApiModel(description = "考试答题信息")
public class ExamDetailDTO {

    @ApiModelProperty("题目id")
    private Long questionId;

    @ApiModelProperty("题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题")
    private Integer questionType;

    @ApiModelProperty("学员答案")
    private String answer;
}
