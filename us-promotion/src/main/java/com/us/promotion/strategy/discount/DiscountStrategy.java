package com.us.promotion.strategy.discount;

import com.us.promotion.enums.DiscountType;

import java.util.EnumMap;

/**
 * 折扣策略工厂：根据优惠券类型获取对应的规则实现（简单工厂 + 枚举表）
 */
public class DiscountStrategy {

    private final static EnumMap<DiscountType, Discount> strategies;

    static {
        strategies = new EnumMap<>(DiscountType.class);
        strategies.put(DiscountType.NO_THRESHOLD, new NoThresholdDiscount());
        strategies.put(DiscountType.PER_PRICE_DISCOUNT, new PerPriceDiscount());
        strategies.put(DiscountType.RATE_DISCOUNT, new RateDiscount());
        strategies.put(DiscountType.PRICE_DISCOUNT, new PriceDiscount());
    }

    public static Discount getDiscount(DiscountType type) {
        return strategies.get(type);
    }
}
