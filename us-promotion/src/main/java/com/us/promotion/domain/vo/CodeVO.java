package com.us.promotion.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 兑换码 VO（笔记 4.7 练习：返回值只包含 id 和 code 两个字段，课前资料未提供源码，按接口规格自实现）
 */
@Data
@ApiModel(description = "兑换码信息")
public class CodeVO {

    @ApiModelProperty("兑换码id")
    private Integer id;

    @ApiModelProperty("兑换码")
    private String code;
}
