package com.us.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.common.utils.CollUtils;
import com.us.common.utils.DateUtils;
import com.us.common.utils.UserContext;
import com.us.learning.constants.RedisConstants;
import com.us.learning.domain.po.PointsRecord;
import com.us.learning.domain.vo.PointsStatisticsVO;
import com.us.learning.enums.PointsRecordType;
import com.us.learning.mapper.PointsRecordMapper;
import com.us.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import com.us.common.exceptions.BizIllegalException;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {


    /**
     * 新增积分记录（day07 笔记 3.1.4：先判断每日上限，再按实际可得分保存）
     */
    @Override
    @Transactional
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        addPointsRecord(userId, points, type, UUID.randomUUID().toString());
    }

    @Override
    @Transactional
    public void addPointsRecord(Long userId, int points, PointsRecordType type, String eventId) {
        if (eventId == null || eventId.length() > 64 || userId == null || points <= 0) {
            throw new BizIllegalException("积分事件参数不合法");
        }
        if (baseMapper.recordEvent(eventId, userId) == 0) return;
        baseMapper.lockDailyPoints(userId, type.getValue());
        LocalDateTime now = LocalDateTime.now();
        int maxPoints = type.getMaxPoints();
        // 1.判断当前方式有没有积分上限
        int realPoints = points;
        if(maxPoints > 0) {
            // 2.有，则需要判断是否超过上限
            LocalDateTime begin = DateUtils.getDayStartTime(now);
            LocalDateTime end = DateUtils.getDayEndTime(now);
            // 2.1.查询今日已得积分
            int currentPoints = queryUserPointsByTypeAndDate(userId, type, begin, end);
            // 2.2.判断是否超过上限
            if(currentPoints >= maxPoints) {
                // 2.3.超过，直接结束
                return;
            }
            // 2.4.没超过，计算实际可得积分（可能只能补到上限）
            if(currentPoints + points > maxPoints){
                realPoints = maxPoints - currentPoints;
            }
        }
        // 3.保存积分记录
        PointsRecord p = new PointsRecord();
        p.setPoints(realPoints);
        p.setUserId(userId);
        p.setType(type);
        save(p);
        // 与积分记录同一事务保存待同步标记，由后台任务按数据库总分覆盖排行榜。
        baseMapper.markBoardDirty(userId, now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER));
    }

    private int queryUserPointsByTypeAndDate(
            Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        // 1.查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .eq(type != null, PointsRecord::getType, type)
                .between(begin != null && end != null, PointsRecord::getCreateTime, begin, end);
        // 2.调用mapper，查询结果
        Integer points = getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        // 3.判断并返回
        return points == null ? 0 : points;
    }

    /**
     * 查询我的今日积分情况（day07 笔记 3.2.2）
     */
    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.获取日期范围
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        // 3.构建查询条件
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId, userId)
                .between(PointsRecord::getCreateTime, begin, end);
        // 4.查询（按类型分组统计）
        List<PointsRecord> list = getBaseMapper().queryUserPointsByDate(wrapper);
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 5.封装返回
        List<PointsStatisticsVO> vos = new ArrayList<>(list.size());
        for (PointsRecord p : list) {
            PointsStatisticsVO vo = new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }
}