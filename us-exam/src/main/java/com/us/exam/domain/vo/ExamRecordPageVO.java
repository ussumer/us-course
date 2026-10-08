package com.us.exam.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试记录列表项（day13~14 笔记 3.2.3）
 */
@Data
@ApiModel(description = "考试记录列表项")
public class ExamRecordPageVO {

    @ApiModelProperty("考试记录id")
    private Long id;

    @ApiModelProperty("考试类型，1：练习，2：考试")
    private Integer type;

    @ApiModelProperty("考试得分")
    private Integer score;

    @ApiModelProperty("提交时间")
    private LocalDateTime commitTime;

    @ApiModelProperty("考试用时，单位秒")
    private Integer duration;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("小节名称")
    private String sectionName;
}
