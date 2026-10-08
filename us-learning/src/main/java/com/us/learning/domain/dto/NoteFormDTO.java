package com.us.learning.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 新增笔记的表单实体（day13~14 笔记 3.1.1）
 */
@Data
@ApiModel(description = "新增笔记表单")
public class NoteFormDTO {

    @ApiModelProperty("笔记内容")
    @NotNull(message = "笔记内容不能为空")
    @Size(max = 2048, message = "笔记内容过长")
    private String content;

    @ApiModelProperty("是否为私密笔记，true：私密")
    private Boolean isPrivate;

    @ApiModelProperty("笔记记录时的视频播放时间点，单位秒")
    private Integer noteMoment;

    @ApiModelProperty("课程id")
    @NotNull(message = "课程id不能为空")
    private Long courseId;

    @ApiModelProperty("章id")
    private Long chapterId;

    @ApiModelProperty("小节id")
    private Long sectionId;
}
