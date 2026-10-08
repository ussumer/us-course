package com.us.promotion.service;

import com.us.api.dto.promotion.CouponDiscountDTO;
import com.us.api.dto.promotion.OrderCouponDTO;
import com.us.api.dto.promotion.OrderCourseDTO;

import java.util.List;

/**
 * 优惠方案计算服务（day12）
 */
public interface IDiscountService {

    /**
     * 查询我的优惠券可用方案（day12 笔记 2）
     *
     * @param orderCourses 订单中的课程信息
     * @return 最优优惠方案集合
     */
    List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses);

    /**
     * 根据券方案计算订单优惠明细（day12 笔记 3.1）
     *
     * @param orderCouponDTO 订单课程及所选用户券
     * @return 优惠明细（含每个课程优惠金额）
     */
    CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO);
}
