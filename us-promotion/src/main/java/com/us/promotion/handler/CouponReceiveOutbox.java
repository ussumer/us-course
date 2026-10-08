package com.us.promotion.handler;

import com.us.common.autoconfigure.mq.RabbitMqHelper;
import com.us.common.constants.MqConstants;
import com.us.common.utils.JsonUtils;
import com.us.promotion.domain.dto.UserCouponDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static com.us.promotion.service.impl.UserCouponServiceImpl.OUTBOX_DATA_KEY;
import static com.us.promotion.service.impl.UserCouponServiceImpl.OUTBOX_PENDING_KEY;

/** 库存预扣时保存的事件，收到 broker 确认且成功路由后才移除。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReceiveOutbox {
    private static final RedisScript<Long> ACK = RedisScript.of(
            new ClassPathResource("lua/ack_coupon_event.lua"), Long.class);
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;

    @Scheduled(fixedDelayString = "${us.promotion.outbox-interval-ms:2000}")
    public void publishPendingEvents() {
        Set<String> ids = redisTemplate.opsForZSet().range(OUTBOX_PENDING_KEY, 0, 49);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            try {
                Object json = redisTemplate.opsForHash().get(OUTBOX_DATA_KEY, id);
                if (json == null) {
                    continue;
                }
                UserCouponDTO event = JsonUtils.toBean(json.toString(), UserCouponDTO.class);
                mqHelper.sendConfirmed(MqConstants.Exchange.PROMOTION_EXCHANGE,
                        MqConstants.Key.COUPON_RECEIVE, event, id);
                redisTemplate.execute(ACK, List.of(OUTBOX_DATA_KEY, OUTBOX_PENDING_KEY), id);
            } catch (Exception e) {
                log.warn("领券事件{}投递失败，保留到下一轮重试", id, e);
                break;
            }
        }
    }
}
