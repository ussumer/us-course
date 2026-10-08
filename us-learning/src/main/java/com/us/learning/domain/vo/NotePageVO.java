package com.us.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端笔记列表项（day13~14 笔记 3.1.5）
 */
@Data
@ApiModel(description = "管理端笔记列表项")
public class NotePageVO {

    @ApiModelProperty("笔记id")
    private Long id;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("章名称")
    private String chapterName;

    @ApiModelProperty("节名称")
    private String sectionName;

    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("是否在用户端隐藏")
    private Boolean hidden;

    @ApiModelProperty("被采集次数")
    private Integer usedTimes;

    @ApiModelProperty("笔记作者")
    private String authorName;

    @ApiModelProperty("笔记发布时间")
    private LocalDateTime createTime;
}
