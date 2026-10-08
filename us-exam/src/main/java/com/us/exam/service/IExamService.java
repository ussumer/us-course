package com.us.exam.service;

import com.us.common.domain.dto.PageDTO;
import com.us.common.domain.query.PageQuery;
import com.us.exam.domain.dto.ExamCommitDTO;
import com.us.exam.domain.dto.ExamFormDTO;
import com.us.exam.domain.vo.ExamRecordDetailVO;
import com.us.exam.domain.vo.ExamRecordPageVO;
import com.us.exam.domain.vo.ExamVO;

import java.util.List;

/**
 * 考试评测服务（day13~14 分组实战）
 */
public interface IExamService {

    /**
     * 获取试题并开始考试（笔记 3.2.1）
     *
     * @param dto 课程id、小节id、考试类型
     * @return 考试记录id + 题目列表
     */
    ExamVO getQuestionsAndBeginExam(ExamFormDTO dto);

    /**
     * 提交考试结果（笔记 3.2.2）：自动批阅、保存答题信息、通知学习微服务
     *
     * @param dto 考试记录id + 答题信息
     */
    void commitExam(ExamCommitDTO dto);

    /**
     * 分页查询我的考试记录（笔记 3.2.3）
     *
     * @param query 分页参数
     * @return 考试记录分页结果
     */
    PageDTO<ExamRecordPageVO> queryExamRecordPage(PageQuery query);

    /**
     * 查询考试记录详情（笔记 3.2.4）
     *
     * @param id 考试记录id
     * @return 每道题的作答信息与题目信息
     */
    List<ExamRecordDetailVO> queryExamRecordDetail(Long id);
}
