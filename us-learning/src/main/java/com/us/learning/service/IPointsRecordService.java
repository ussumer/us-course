package com.us.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.learning.domain.po.PointsRecord;
import com.us.learning.domain.vo.PointsStatisticsVO;
import com.us.learning.enums.PointsRecordType;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 */
public interface IPointsRecordService extends IService<PointsRecord> {

    /**
     * 新增积分记录（带每日上限判断）
     * @param userId 用户id
     * @param points 本次积分
     * @param type 积分类型（含每日上限）
     */
    void addPointsRecord(Long userId, int points, PointsRecordType type);

    void addPointsRecord(Long userId, int points, PointsRecordType type, String eventId);

    /**
     * 查询我的今日积分情况（按类型分组）
     * @return 各类型的今日积分与上限
     */
    List<PointsStatisticsVO> queryMyPointsToday();
}
