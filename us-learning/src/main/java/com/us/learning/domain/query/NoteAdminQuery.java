package com.us.learning.domain.query;

import com.us.common.domain.query.PageQuery;
import com.us.common.utils.DateUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理端笔记分页查询参数（day13~14 笔记 3.1.5）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "管理端笔记查询参数")
public class NoteAdminQuery extends PageQuery {

    @ApiModelProperty("课程名称关键字")
    private String name;

    @ApiModelProperty("笔记状态，是否隐藏，true：隐藏")
    private Boolean hidden;

    @ApiModelProperty("笔记发布时间的开始时间")
    @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_TIME_FORMAT)
    private LocalDateTime beginTime;

    @ApiModelProperty("笔记发布时间的结束时间")
    @DateTimeFormat(pattern = DateUtils.DEFAULT_DATE_TIME_FORMAT)
    private LocalDateTime endTime;
}
