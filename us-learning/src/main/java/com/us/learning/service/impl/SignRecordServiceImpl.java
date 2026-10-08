package com.us.learning.service.impl;

import com.us.common.autoconfigure.mq.RabbitMqHelper;
import com.us.common.constants.MqConstants;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.utils.BooleanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.DateUtils;
import com.us.common.utils.UserContext;
import com.us.learning.constants.RedisConstants;
import com.us.learning.domain.vo.SignRecordVO;
import com.us.learning.domain.vo.SignResultVO;
import com.us.learning.mq.message.SignInMessage;
import com.us.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 签到服务实现（day07 笔记 2.3.4 + 3.1.2）
 * 核心：用 Redis BitMap 实现签到，每个月一个KEY，第N天对应第N-1个bit位
 */
@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqHelper mqHelper;

    @Override
    public SignResultVO addSignRecords() {
        // 1.签到
        // 1.1.获取登录用户
        Long userId = UserContext.getUser();
        // 1.2.获取日期
        LocalDate now = LocalDate.now();
        // 1.3.拼接key
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX
                + userId
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        // 1.4.计算offset
        int offset = now.getDayOfMonth() - 1;
        // 1.5.保存签到信息（SETBIT，返回旧值true代表今天已经签过了）
        Boolean exists = redisTemplate.opsForValue()
                .setBit(key, offset, true);
        if (BooleanUtils.isTrue(exists)) {
            throw new BizIllegalException("不允许重复签到！");
        }
        // 2.计算连续签到天数
        int signDays = countSignDays(key, now.getDayOfMonth());
        // 3.计算签到得分（连续7/14/28天有额外奖励）
        int rewardPoints = 0;
        switch (signDays) {
            case 7:
                rewardPoints = 10;
                break;
            case 14:
                rewardPoints = 20;
                break;
            case 28:
                rewardPoints = 40;
                break;
        }
        // 4.保存积分明细记录（发送MQ，由积分服务异步处理）
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId, rewardPoints + 1)
        );// 签到积分是基本得分+奖励积分
        // 5.封装返回
        SignResultVO vo = new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    /**
     * 统计连续签到天数：从今天往前数，遇到第一个未签到的bit位停止
     * 二进制遍历技巧：与1做与运算取最后一位，右移一位丢弃它，循环直到遇到0
     */
    private int countSignDays(String key, int len) {
        // 1.获取本月从第一天开始，到今天为止的所有签到记录（BITFIELD GET u[len] 0）
        List<Long> result = redisTemplate.opsForValue()
                .bitField(
                        key,
                        BitFieldSubCommands.create()
                                .get(
                                        BitFieldSubCommands.BitFieldType.unsigned(len))
                                .valueAt(0)
                );
        if (CollUtils.isEmpty(result)) {
            return 0;
        }
        int num = result.get(0)
                .intValue();
        // 2.定义一个计数器
        int count = 0;
        // 3.循环，与1做与运算，得到最后一个bit，判断是否为0，为0则终止，为1则继续
        while ((num & 1) == 1) {
            // 4.计数器+1
            count++;
            // 5.把数字右移一位，最后一位被舍弃，倒数第二位成了最后一位
            num >>>= 1;
        }
        return count;
    }

    /**
     * 查询本月签到记录（day07 练习4.1，由学员实现）
     */
    @Override
    public SignRecordVO querySignRecords() {
        Long userId = UserContext.getUser();
        LocalDate now = LocalDate.now();
        String key = RedisConstants.SIGN_RECORD_KEY_PREFIX
                + userId
                + now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        int dayOfMonth = now.getDayOfMonth();
        List<Long> result = redisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (CollUtils.isEmpty(result)) {
            return null;
        }
        long num = result.get(0);
        int signDays = countSignDays(key, dayOfMonth);
        List<Byte> signRecords = new ArrayList<>(dayOfMonth);
        for (int i = dayOfMonth - 1; i >= 0; i--) {
            signRecords.add((byte) ((num >> i) & 1));
        }
        SignRecordVO vo = new SignRecordVO();
        vo.setSignDays(signDays);
        vo.setSignRecords(signRecords);
        return vo;
    }
}