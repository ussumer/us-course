package com.us.promotion.handler;

import com.us.promotion.domain.dto.UserCouponDTO;
import com.us.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import com.us.common.exceptions.BizIllegalException;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.us.common.constants.MqConstants.Exchange.PROMOTION_EXCHANGE;
import static com.us.common.constants.MqConstants.Key.COUPON_RECEIVE;

/**
 * 促销服务MQ监听器（day11 笔记 2.4.3）
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PromotionMqHandler {

    private final IUserCouponService userCouponService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "coupon.receive.queue", durable = "true"),
            exchange = @Exchange(name = PROMOTION_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = COUPON_RECEIVE
    ))
    public void listenCouponReceiveMessage(UserCouponDTO uc, Message message) {
        // 兼容升级前已在队列中的消息，使用稳定的 AMQP messageId 去重。
        if (uc.getRequestId() == null) {
            uc.setRequestId(message.getMessageProperties().getMessageId());
        }
        if (uc.getRequestId() == null || uc.getRequestId().length() > 64) {
            throw new BizIllegalException("领取消息缺少有效的事件ID，需人工核对后重放");
        }
        userCouponService.checkAndCreateUserCoupon(uc);
    }
}
