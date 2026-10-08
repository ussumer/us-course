package com.us.learning.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 编辑笔记的表单实体（day13~14 笔记 3.1.3：笔记与课程、章节绑定后不可修改，只能改内容和是否私密）
 */
@Data
@ApiModel(description = "编辑笔记表单")
public class NoteEditDTO {

    @ApiModelProperty("笔记内容")
    @NotNull(message = "笔记内容不能为空")
    private String content;

    @ApiModelProperty("是否为私密笔记，true：私密")
    private Boolean isPrivate;
}
