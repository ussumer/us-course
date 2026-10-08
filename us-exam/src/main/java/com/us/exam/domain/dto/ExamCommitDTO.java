package com.us.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 提交考试结果的表单实体（day13~14 笔记 3.2.2）
 */
@Data
@ApiModel(description = "提交考试结果表单")
public class ExamCommitDTO {

    @ApiModelProperty("考试记录id")
    @NotNull(message = "考试记录id不能为空")
    private Long id;

    @ApiModelProperty("考试答题信息")
    private List<ExamDetailDTO> examDetails;
}
