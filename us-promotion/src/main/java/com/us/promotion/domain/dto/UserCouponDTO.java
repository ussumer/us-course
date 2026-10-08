package com.us.promotion.domain.dto;

import lombok.Data;

/**
 * 异步领券的MQ消息体（day11 笔记 2.4.1）
 */
@Data
public class UserCouponDTO {
    /** 同一次领取事件在重试、重复投递时保持不变。 */
    private String requestId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 优惠券id
     */
    private Long couponId;
    /**
     * 兑换码序列号（day11 练习3.1：兑换码兑换时携带，用于消费时更新兑换码状态）
     */
    private Integer serialNum;
}
