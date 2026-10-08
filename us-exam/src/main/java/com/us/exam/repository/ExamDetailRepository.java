package com.us.exam.repository;

import com.us.exam.domain.mongo.ExamDetail;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 答题详情 Repository（day13~14）
 */
public interface ExamDetailRepository extends MongoRepository<ExamDetail, String> {

    /**
     * 根据考试记录id查询所有答题详情
     */
    List<ExamDetail> findByExamId(Long examId);

    /**
     * 删除某次考试的答题详情（重新作答时清理）
     */
    void deleteByExamId(Long examId);
}
