package com.us.promotion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 促销中心配置类
 */
@Slf4j
@Configuration
public class PromotionConfig {

    /**
     * 兑换码异步生成线程池（笔记 3.3.2）
     * 发放优惠券不是高频业务，线程数无需太高，队列满时交给调用线程处理
     */
    @Bean
    public Executor generateExchangeCodeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(2);
        // 2.最大线程池大小
        executor.setMaxPoolSize(5);
        // 3.队列大小
        executor.setQueueCapacity(200);
        // 4.线程名称
        executor.setThreadNamePrefix("exchange-code-handler-");
        // 5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 优惠方案计算线程池（day12 笔记 2.6）
     * 方案计算是CPU密集型任务，线程数与核数一致，队列满时直接拒绝（避免拖垮服务）
     */
    @Bean
    public Executor discountSolutionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 1.核心线程池大小
        executor.setCorePoolSize(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())));
        // 2.最大线程池大小
        executor.setMaxPoolSize(4);
        // 3.队列大小
        executor.setQueueCapacity(2048);
        // 4.线程名称
        executor.setThreadNamePrefix("discount-solution-calculator-");
        // 5.拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
