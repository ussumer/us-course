package com.us.promotion.constants;

public class PromotionConstants {

    /**
     * 兑换码序列号，全局自增（Redis String）
     * 每张券发放时从这里申请一段连续的序列号
     */
    public static final String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";

    /**
     * 兑换码序列号的范围（Redis ZSet）
     * member：优惠券id，score：该券已生成兑换码的最大序列号
     * 用途：兑换时判断序列号是否属于某张优惠券的兑换码范围
     */
    public static final String COUPON_RANGE_KEY = "coupon:code:range";

    /**
     * 兑换码兑换状态的BitMap（Redis BitMap）
     * 兑换码序列号是几，就操作第几个bit位：1-已兑换，0-未兑换
     */
    public static final String COUPON_CODE_MAP_KEY = "coupon:code:map";

    /**
     * 优惠券缓存KEY前缀（Redis Hash）
     * field：issueBeginTime、issueEndTime、totalNum、userLimit
     */
    public static final String COUPON_CACHE_KEY_PREFIX = "prs:coupon:";

    /**
     * 用户已领取数量缓存KEY前缀（Redis Hash）
     * field：userId，value：该用户已领取数量
     */
    public static final String USER_COUPON_CACHE_KEY_PREFIX = "prs:user:coupon:";

    /**
     * 领券资格校验失败的错误提示，角标对应LUA脚本的返回值-1（返回值1~4）
     */
    public static final String[] RECEIVE_COUPON_ERROR_MSG = {
            "优惠券不存在",
            "优惠券库存不足",
            "优惠券发放已经结束或尚未开始",
            "超出领取数量"
    };

    /**
     * 兑换码兑换资格校验失败的错误提示，角标对应LUA脚本的返回值-1（返回值1~5）
     */
    public static final String[] EXCHANGE_COUPON_ERROR_MSG = {
            "兑换码已经被兑换过了",
            "兑换码不存在",
            "优惠券不存在",
            "优惠券活动未开始或已经结束",
            "超出领取数量"
    };
}
