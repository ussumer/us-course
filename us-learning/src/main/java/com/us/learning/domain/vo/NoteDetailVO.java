package com.us.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端笔记详情（day13~14 笔记 3.1.6）
 */
@Data
@ApiModel(description = "管理端笔记详情")
public class NoteDetailVO {

    @ApiModelProperty("笔记id")
    private Long id;

    @ApiModelProperty("课程名称")
    private String courseName;

    @ApiModelProperty("章名称")
    private String chapterName;

    @ApiModelProperty("节名称")
    private String sectionName;

    @ApiModelProperty("课程分类名称，多级用/拼接")
    private String categoryNames;

    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("笔记记录时的视频时间点，单位秒")
    private Integer noteMoment;

    @ApiModelProperty("是否在用户端隐藏")
    private Boolean hidden;

    @ApiModelProperty("被采集次数")
    private Integer usedTimes;

    @ApiModelProperty("作者名称")
    private String authorName;

    @ApiModelProperty("作者电话")
    private String authorPhone;

    @ApiModelProperty("笔记发布时间")
    private LocalDateTime createTime;

    @ApiModelProperty("采集人名称集合")
    private List<String> gathers;
}
