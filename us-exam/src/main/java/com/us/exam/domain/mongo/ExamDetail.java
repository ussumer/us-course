package com.us.exam.domain.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

/**
 * 考试答题详情（MongoDB，day13~14 分组实战）
 * 一次考试中每一道题的作答记录
 */
@Data
@Document("exam_detail")
public class ExamDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    private String id;

    /**
     * 所属考试记录id
     */
    private Long examId;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题目类型，1：单选题，2：多选题，3：不定向选择题，4：判断题，5：主观题
     */
    private Integer questionType;

    /**
     * 学员答案
     */
    private String answer;

    /**
     * 是否正确
     */
    private Boolean correct;

    /**
     * 本题得分
     */
    private Integer score;

    /**
     * 老师评语
     */
    private String comment;
}
