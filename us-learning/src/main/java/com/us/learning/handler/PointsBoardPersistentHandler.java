package com.us.learning.handler;

import com.us.common.utils.CollUtils;
import com.us.common.utils.DateUtils;
import com.us.learning.constants.RedisConstants;
import com.us.learning.domain.po.PointsBoard;
import com.us.learning.service.IPointsBoardSeasonService;
import com.us.learning.service.IPointsBoardService;
import com.us.learning.utils.TableInfoContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.us.learning.constants.LearningConstants.POINTS_BOARD_TABLE_PREFIX;

/**
 * 榜单持久化任务（day08 笔记 2.3.1 / 2.4.2 / 2.4.3 / 2.4.4）
 * 三个任务通过XXL-JOB的子任务机制串联执行
 */
@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {

    private final IPointsBoardSeasonService seasonService;

    private final IPointsBoardService pointsBoardService;

    private final StringRedisTemplate redisTemplate;

    /**
     * 每月1号凌晨3点执行：创建上赛季的榜单表
     */
    @XxlJob("createPointsBoardTableOfLastSeason")
    public void createPointsBoardTableOfLastSeason(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.查询赛季id
        Integer season = seasonService.querySeasonByTime(time);
        if (season == null) {
            // 赛季不存在
            return;
        }
        // 3.创建表
        pointsBoardService.createPointsBoardTableBySeason(season);
    }

    /**
     * 持久化上赛季Redis榜单数据到数据库（支持XXL-JOB分片广播，多执行器并行不重复）
     */
    @XxlJob("savePointsBoard2DB")
    public void savePointsBoard2DB(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.计算动态表名
        // 2.1.查询赛季信息
        Integer season = seasonService.querySeasonByTime(time);
        // 2.2.存入ThreadLocal
        if (season == null) throw new IllegalStateException("上赛季不存在，不能持久化榜单");
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + season);
        try {
            // 3.查询榜单数据
            // 3.1.拼接KEY
            String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
            // 3.2.查询数据（分片广播：起始页=分片序号+1，跨度=总分片数）
            int index = XxlJobHelper.getShardIndex();
            int total = XxlJobHelper.getShardTotal();
            int pageNo = index + 1;
            int pageSize = 1000;
            while (true) {
                List<PointsBoard> boardList = pointsBoardService.queryCurrentBoardList(key, pageNo, pageSize);
                if (CollUtils.isEmpty(boardList)) {
                    break;
                }
                // 4.持久化到数据库
                // 4.1.把排名信息写入id
                boardList.forEach(b -> {
                    b.setId(b.getRank().longValue());
                    b.setRank(null);
                });
                // 4.2.持久化
                pointsBoardService.saveOrUpdateBatch(boardList);
                // 5.翻页，跳过N个页，N就是分片数量
                pageNo += total;
            }
            // 任务结束，移除动态表名
        } finally {
            TableInfoContext.remove();
        }
    }

    /**
     * 持久化完成后，清理Redis中上赛季的榜单数据
     */
    @XxlJob("clearPointsBoardFromRedis")
    public void clearPointsBoardFromRedis(){
        // 1.获取上月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        // 2.计算key
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + time.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 3.删除（unlink异步删除，避免大key阻塞）
        redisTemplate.unlink(key);
    }
}