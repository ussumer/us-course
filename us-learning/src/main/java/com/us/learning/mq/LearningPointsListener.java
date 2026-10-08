package com.us.learning.mq;

import com.us.common.constants.MqConstants;
import com.us.learning.enums.PointsRecordType;
import com.us.learning.mq.message.SignInMessage;
import com.us.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 学习积分消息监听器（day07 笔记 3.1.3 + 练习4.2）
 * 每种积分行为对应一个队列+RoutingKey，互不干扰
 */
@Component
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService recordService;

    /**
     * 监听新增互动问答事件（写回答得5分，每日上限20）
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))
    public void listenWriteReplyMessage(Long userId, Message delivery){
        recordService.addPointsRecord(userId, 5, PointsRecordType.QA, delivery.getMessageProperties().getMessageId());
    }

    /**
     * 监听签到事件（签到积分由业务方通过消息传递，因为有连续签到奖励）
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "sign.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN
    ))
    public void listenSignInMessage(SignInMessage message, Message delivery){
        recordService.addPointsRecord(message.getUserId(), message.getPoints(), PointsRecordType.SIGN, delivery.getMessageProperties().getMessageId());
    }

    /**
     * 监听学习完成事件（学完一小节得10分，每日上限50）——day07 练习4.2
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "learning.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION
    ))
    public void listenLearnSectionMessage(Long userId, Message delivery){
        recordService.addPointsRecord(userId, 10, PointsRecordType.LEARNING, delivery.getMessageProperties().getMessageId());
    }

    /**
     * 监听写笔记事件（写笔记得5分，每日上限20）——day13~14 笔记功能
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "note.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_NOTE
    ))
    public void listenWriteNoteMessage(Long userId, Message delivery){
        recordService.addPointsRecord(userId, 5, PointsRecordType.NOTE, delivery.getMessageProperties().getMessageId());
    }
}