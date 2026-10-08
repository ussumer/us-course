package com.us.api.dto.exam;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试完成消息（day13~14：考试服务提交试卷后，通知学习微服务记录学习进度）
 */
@Data
@ApiModel(description = "考试完成消息")
public class ExamFinishedDTO {

    @ApiModelProperty("学员id")
    private Long userId;

    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("小节id")
    private Long sectionId;

    @ApiModelProperty("考试得分")
    private Integer score;

    @ApiModelProperty("考试用时，单位秒")
    private Integer duration;

    @ApiModelProperty("交卷时间")
    private LocalDateTime finishTime;
}
