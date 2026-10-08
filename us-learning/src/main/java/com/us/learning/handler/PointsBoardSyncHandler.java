package com.us.learning.handler;

import com.us.learning.constants.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** MySQL 保存事实与待同步标记，Redis 排行榜是可重新构建的视图。 */
@Slf4j
@Component
public class PointsBoardSyncHandler {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final TransactionTemplate transactions;

    public PointsBoardSyncHandler(JdbcTemplate jdbc, StringRedisTemplate redis, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.transactions = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelay = 2000)
    public void synchronizeBoards() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT user_id, board_month FROM points_board_sync LIMIT 100");
        for (Map<String, Object> row : rows) {
            try {
                transactions.execute(status -> {
                    long userId = ((Number) row.get("user_id")).longValue();
                    String month = row.get("board_month").toString();
                    List<Long> locked = jdbc.queryForList(
                            "SELECT user_id FROM points_board_sync WHERE user_id=? AND board_month=? FOR UPDATE", Long.class, userId, month);
                    if (locked.isEmpty()) return null;
                    LocalDate from = LocalDate.parse(month + "01", DateTimeFormatter.BASIC_ISO_DATE);
                    Integer total = jdbc.queryForObject(
                            "SELECT COALESCE(SUM(points),0) FROM points_record WHERE user_id=? AND create_time>=? AND create_time<?",
                            Integer.class, userId, from.toString(), from.plusMonths(1).toString());
                    redis.opsForZSet().add(RedisConstants.POINTS_BOARD_KEY_PREFIX + month, Long.toString(userId), total);
                    jdbc.update("DELETE FROM points_board_sync WHERE user_id=? AND board_month=?", userId, month);
                    return null;
                });
            } catch (Exception e) {
                log.warn("排行榜同步失败，保留数据库待同步标记", e);
            }
        }
    }
}
