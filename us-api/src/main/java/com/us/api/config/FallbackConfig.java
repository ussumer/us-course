package com.us.api.config;

import com.us.api.client.learning.fallback.LearningClientFallback;
import com.us.api.client.promotion.fallback.PromotionClientFallback;
import com.us.api.client.remark.fallback.RemarkClientFallback;
import com.us.api.client.trade.fallback.TradeClientFallback;
import com.us.api.client.user.fallback.UserClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FallbackConfig {
    @Bean
    public LearningClientFallback learningClientFallback(){
        return new LearningClientFallback();
    }

    @Bean
    public TradeClientFallback tradeClientFallback(){
        return new TradeClientFallback();
    }

    @Bean
    public UserClientFallback userClientFallback(){
        return new UserClientFallback();
    }

    @Bean
    public RemarkClientFallback remarkClientFallback(){
        return new RemarkClientFallback();
    }

    /**
     * 促销服务降级工厂（缺少此Bean时，开启熔断的场景下注入PromotionClient会启动失败）
     */
    @Bean
    public PromotionClientFallback promotionClientFallback(){
        return new PromotionClientFallback();
    }
}
