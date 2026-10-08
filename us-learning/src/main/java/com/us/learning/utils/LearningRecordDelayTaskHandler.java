package com.us.learning.utils;

import com.us.common.utils.JsonUtils;
import com.us.learning.domain.po.LearningRecord;
import com.us.learning.mapper.LearningRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Redis 保存合并后的进度和到期索引；实例重启后仍可继续处理待写入记录。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningRecordDelayTaskHandler {
    private static final String PENDING_DATA = "learning:record:pending:data";
    private static final String PENDING_DUE = "learning:record:pending:due";
    private static final RedisScript<Long> ENQUEUE = RedisScript.of(
            new ClassPathResource("lua/enqueue_progress.lua"), Long.class);
    private static final RedisScript<Long> ACK = RedisScript.of(
            new ClassPathResource("lua/ack_progress.lua"), Long.class);
    private static final RedisScript<Long> CLEAR = RedisScript.of(
            new ClassPathResource("lua/clear_progress.lua"), Long.class);
    private final StringRedisTemplate redisTemplate;
    private final LearningRecordMapper recordMapper;
    /** Direct mode is a benchmark baseline; normal deployments coalesce writes. */
    @Value("${us.learning.progress-direct-write:false}")
    private boolean directWrite;

    @Scheduled(fixedDelay = 2000)
    public void handleDelayTask() {
        Set<String> members = redisTemplate.opsForZSet().rangeByScore(
                PENDING_DUE, 0, System.currentTimeMillis(), 0, 100);
        if (members == null) return;
        for (String member : members) {
            try {
                Object value = redisTemplate.opsForHash().get(PENDING_DATA, member);
                if (value == null) continue;
                String json = value.toString();
                LearningRecord record = JsonUtils.toBean(json, LearningRecord.class);
                // 不覆盖已经完成的记录；两条 SQL 任一步失败都保留任务。
                recordMapper.updatePendingMoment(record.getId(), record.getMoment());
                recordMapper.updateLatestLearning(record.getLessonId(), record.getSectionId());
                redisTemplate.execute(ACK, List.of(PENDING_DATA, PENDING_DUE), member, json);
            } catch (Exception e) {
                log.warn("学习进度{}持久化失败，保留任务重试", member, e);
            }
        }
    }

    public void addLearningRecordTask(LearningRecord record) {
        if (directWrite) {
            recordMapper.updatePendingMoment(record.getId(), record.getMoment());
            recordMapper.updateLatestLearning(record.getLessonId(), record.getSectionId());
            return;
        }
        redisTemplate.execute(ENQUEUE,
                List.of(cacheKey(record.getLessonId()), PENDING_DATA, PENDING_DUE),
                record.getSectionId().toString(), member(record.getLessonId(), record.getSectionId()),
                JsonUtils.toJsonStr(record), String.valueOf(System.currentTimeMillis() + 20_000));
    }

    public void writeRecordCache(LearningRecord record) {
        String key = cacheKey(record.getLessonId());
        // 查询数据库后的回填不能覆盖另一个请求已经提交的新进度。
        redisTemplate.opsForHash().putIfAbsent(key, record.getSectionId().toString(), JsonUtils.toJsonStr(record));
        redisTemplate.expire(key, Duration.ofMinutes(1));
    }

    public LearningRecord readRecordCache(Long lessonId, Long sectionId) {
        Object json = redisTemplate.opsForHash().get(cacheKey(lessonId), sectionId.toString());
        return json == null ? null : JsonUtils.toBean(json.toString(), LearningRecord.class);
    }

    public void cleanRecordCache(Long lessonId, Long sectionId) {
        redisTemplate.execute(CLEAR, List.of(cacheKey(lessonId), PENDING_DATA, PENDING_DUE),
                sectionId.toString(), member(lessonId, sectionId));
    }

    private String cacheKey(Long lessonId) { return "learning:record:" + lessonId; }
    private String member(Long lessonId, Long sectionId) { return lessonId + ":" + sectionId; }
}
