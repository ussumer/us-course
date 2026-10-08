package com.us.exam.domain.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试记录（MongoDB，day13~14 分组实战：考试评测）
 * 一次练习/考试对应一条记录
 */
@Data
@Document("exam_record")
public class ExamRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 考试记录id（雪花算法生成，交卷时使用）
     */
    @Id
    private Long id;

    /**
     * 学员id
     */
    private Long userId;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 小节id
     */
    private Long sectionId;

    /**
     * 课程名称（冗余字段，列表页直接展示）
     */
    private String courseName;

    /**
     * 小节名称（冗余字段）
     */
    private String sectionName;

    /**
     * 考试类型，1：练习，2：考试
     */
    private Integer type;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 试卷总分
     */
    private Integer totalScore;

    /**
     * 学员得分
     */
    private Integer score;

    /**
     * 正确题目数量
     */
    private Integer correctCount;

    /**
     * 考试用时，单位秒
     */
    private Integer duration;

    /**
     * 开始考试时间
     */
    private LocalDateTime createTime;

    /**
     * 交卷时间
     */
    private LocalDateTime commitTime;

    /**
     * 是否已交卷
     */
    private Boolean finished;
}
