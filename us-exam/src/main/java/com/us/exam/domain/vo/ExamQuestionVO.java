package com.us.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 考试题目信息（day13~14）
 * 开始考试时只返回题目与选项；查询考试详情时额外返回正确答案与解析
 */
@Data
@ApiModel(description = "考试题目信息")
public class ExamQuestionVO {

    @ApiModelProperty("题目id")
    private Long id;

    @ApiModelProperty("题目名称")
    private String name;

    @ApiModelProperty("题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题")
    private Integer type;

    @ApiModelProperty("题目分值")
    private Integer score;

    @ApiModelProperty("选择题选项")
    private List<String> options;

    @ApiModelProperty("难易度，1：简单，2：中等，3：困难")
    private Integer difficulty;

    @ApiModelProperty("正确答案（仅查询考试详情时返回）")
    private String answer;

    @ApiModelProperty("答案解析（仅查询考试详情时返回）")
    private String analysis;
}
