package com.us.learning.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端笔记信息（day13~14 笔记 3.1.4）
 */
@Data
@ApiModel(description = "用户端笔记信息")
public class NoteVO {

    @ApiModelProperty("笔记id")
    private Long id;

    @ApiModelProperty("笔记内容")
    private String content;

    @ApiModelProperty("笔记记录时的视频时间点，单位秒")
    private Integer noteMoment;

    @ApiModelProperty("是否是隐私笔记")
    private Boolean isPrivate;

    @ApiModelProperty("是否是我采集的笔记")
    private Boolean isGathered;

    @ApiModelProperty("作者id")
    private Long authorId;

    @ApiModelProperty("作者名字")
    private String authorName;

    @ApiModelProperty("作者头像")
    private String authorIcon;

    @ApiModelProperty("笔记发布时间")
    private LocalDateTime createTime;
}
