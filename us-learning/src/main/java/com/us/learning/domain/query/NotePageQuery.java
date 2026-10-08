package com.us.learning.domain.query;

import com.us.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端笔记分页查询参数（day13~14 笔记 3.1.4）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "用户端笔记查询参数")
public class NotePageQuery extends PageQuery {

    @ApiModelProperty("课程id")
    private Long courseId;

    @ApiModelProperty("小节id")
    private Long sectionId;

    @ApiModelProperty("是否只查询我的笔记，false：查询全部（含他人公开笔记）")
    private Boolean onlyMine;
}
