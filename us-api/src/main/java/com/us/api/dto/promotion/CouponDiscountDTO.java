package com.us.api.dto.promotion;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel(description = "订单的可用优惠券及折扣信息")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CouponDiscountDTO {
    @ApiModelProperty("用户优惠券id集合")
    private List<Long> ids = new ArrayList<>();
    @ApiModelProperty("优惠券规则")
    private List<String> rules = new ArrayList<>();
    @ApiModelProperty("本订单最大优惠金额")
    private Integer discountAmount = 0;
    /**
     * 优惠明细：key=课程id，value=该课程优惠金额
     * （day12 笔记 3.1.3：计算订单优惠明细时新增的字段）
     */
    @ApiModelProperty("课程优惠明细")
    private Map<Long, Integer> discountDetail = new HashMap<>();
}
