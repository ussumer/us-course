package com.us.promotion.handler;

import com.us.promotion.service.ICouponService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 优惠券相关的定时任务（day09 练习 4.4 / 4.5）
 */
@Component
@RequiredArgsConstructor
public class CouponTask {

    private final ICouponService couponService;

    /**
     * 定时发放优惠券：每分钟（或按需配置）检查一次，把到点的券从"未开始"改为"发放中"
     */
    @XxlJob("beginIssueCoupon")
    public void beginIssueCoupon() {
        // 获取数据分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        couponService.beginIssueCoupon(shardIndex, shardTotal);
    }

    /**
     * 定时结束发放：把过了领用截止时间的券从"发放中"改为"发放结束"
     */
    @XxlJob("finishIssueCoupon")
    public void finishIssueCoupon() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        couponService.finishIssueCoupon(shardIndex, shardTotal);
    }
}
