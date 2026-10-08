package com.us.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.promotion.domain.dto.UserCouponDTO;
import com.us.promotion.domain.po.UserCoupon;
import com.us.promotion.domain.query.UserCouponQuery;
import com.us.promotion.domain.vo.UserCouponVO;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务类
 * </p>
 */
public interface IUserCouponService extends IService<UserCoupon> {

    /**
     * 领取优惠券（day10 手动领取，day11 改为 Redis LUA 校验 + MQ 异步领取）
     *
     * @param couponId 优惠券id
     */
    void receiveCoupon(Long couponId);

    /**
     * 兑换码兑换优惠券（day10 兑换，day11 改为 Redis LUA 校验 + MQ 异步领取）
     *
     * @param code 兑换码
     */
    void exchangeCoupon(String code);

    /**
     * 领券并生成用户券（day10 课堂实现，day11 改为MQ消费入口）
     *
     * @param uc 领券消息（用户id、优惠券id、兑换码序列号）
     */
    void checkAndCreateUserCoupon(UserCouponDTO uc);

    /**
     * 核销优惠券（day12 笔记 3.2）
     *
     * @param userCouponIds 用户券id集合
     */
    void writeOffCoupon(List<Long> userCouponIds);

    /**
     * 退还优惠券（day12 笔记 3.3）
     *
     * @param userCouponIds 用户券id集合
     */
    void refundCoupon(List<Long> userCouponIds);

    /**
     * 查询优惠券规则描述（day12 笔记 3.4）
     *
     * @param userCouponIds 用户券id集合
     * @return 规则描述集合
     */
    List<String> queryDiscountRules(List<Long> userCouponIds);

    /**
     * 分页查询我的优惠券（day10 练习 4.1）
     *
     * @param query 分页与状态条件
     * @return 我的优惠券分页结果
     */
    PageDTO<UserCouponVO> queryMyCoupons(UserCouponQuery query);
}
