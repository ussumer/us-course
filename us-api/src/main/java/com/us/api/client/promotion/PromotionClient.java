package com.us.api.client.promotion;

import com.us.api.client.promotion.fallback.PromotionClientFallback;
import com.us.api.dto.promotion.CouponDiscountDTO;
import com.us.api.dto.promotion.OrderCouponDTO;
import com.us.api.dto.promotion.OrderCourseDTO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "promotion", value = "promotion-service", path = "/user-coupons",
        fallbackFactory = PromotionClientFallback.class)
public interface PromotionClient {

    /**
     * 查询我的优惠券可用方案
     */
    @ApiOperation("查询我的优惠券可用方案")
    @PostMapping("/available")
    List<CouponDiscountDTO> findDiscountSolution(@RequestBody List<OrderCourseDTO> orderCourses);

    /**
     * 根据券方案计算订单优惠明细
     */
    @ApiOperation("根据券方案计算订单优惠明细")
    @PostMapping("/discount")
    CouponDiscountDTO queryDiscountDetailByOrder(@RequestBody OrderCouponDTO orderCouponDTO);

    /**
     * 核销指定优惠券
     */
    @ApiOperation("核销指定优惠券")
    @PutMapping("/use")
    void writeOffCoupon(@RequestParam("couponIds") List<Long> userCouponIds);

    /**
     * 退还指定优惠券
     */
    @ApiOperation("退还指定优惠券")
    @PutMapping("/refund")
    void refundCoupon(@RequestParam("couponIds") List<Long> userCouponIds);

    /**
     * 查询优惠券规则描述
     */
    @ApiOperation("查询优惠券规则描述")
    @GetMapping("/rules")
    List<String> queryDiscountRules(@RequestParam("couponIds") List<Long> userCouponIds);
}
