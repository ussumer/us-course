package com.us.learning.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.us.learning.domain.po.PointsRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 Mapper 接口
 * </p>
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Insert("INSERT IGNORE INTO points_event_record(event_id, user_id) VALUES(#{eventId}, #{userId})")
    int recordEvent(@Param("eventId") String eventId, @Param("userId") Long userId);

    @Insert("INSERT INTO points_daily_lock(user_id, points_type, points_date) VALUES(#{userId}, #{type}, CURDATE()) " +
            "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)")
    int lockDailyPoints(@Param("userId") Long userId, @Param("type") int type);

    @Insert("INSERT INTO points_board_sync(user_id, board_month) VALUES(#{userId}, #{month}) " +
            "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)")
    int markBoardDirty(@Param("userId") Long userId, @Param("month") String month);

    /**
     * 统计用户指定时间段、指定类型的积分总和（用于每日积分上限判断）
     */
    @Select("SELECT SUM(points) FROM points_record ${ew.customSqlSegment}")
    Integer queryUserPointsByTypeAndDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);

    /**
     * 按积分类型分组统计用户指定时间段的积分（用于今日积分情况）
     */
    @Select("SELECT type, SUM(points) AS points FROM points_record ${ew.customSqlSegment} GROUP BY type")
    List<PointsRecord> queryUserPointsByDate(@Param(Constants.WRAPPER) QueryWrapper<PointsRecord> wrapper);
}
