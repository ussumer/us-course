package com.us.learning.service;

import com.us.learning.domain.vo.SignRecordVO;
import com.us.learning.domain.vo.SignResultVO;

public interface ISignRecordService {

    /**
     * 用户签到（day07 笔记 2.3.4：Redis BitMap实现）
     * @return 连续签到天数、签到得分、奖励积分
     */
    SignResultVO addSignRecords();

    /**
     * 查询本月签到记录（day07 练习4.1，由学员实现）
     * @return 本月1号到今天每一天的签到情况（1-已签，0-未签）
     */
    SignRecordVO querySignRecords();
}