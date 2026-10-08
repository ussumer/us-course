package com.us.exam.repository;

import com.us.exam.domain.mongo.ExamRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * 考试记录 Repository（day13~14）
 */
public interface ExamRecordRepository extends MongoRepository<ExamRecord, Long> {

    /**
     * 分页查询某用户已交卷的考试记录
     */
    Page<ExamRecord> findByUserIdAndFinished(Long userId, Boolean finished, Pageable pageable);

    /**
     * 根据id和用户id查询考试记录（防止越权）
     */
    Optional<ExamRecord> findByIdAndUserId(Long id, Long userId);

    /**
     * 判断某用户是否已经参加过某个小节的考试（考试只能参加一次）
     */
    boolean existsByUserIdAndSectionIdAndTypeAndFinished(Long userId, Long sectionId, Integer type, Boolean finished);
}
