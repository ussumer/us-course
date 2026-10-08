package com.us.exam.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.us.api.client.course.CourseClient;
import com.us.api.client.trade.TradeClient;
import com.us.api.dto.course.CatalogueDTO;
import com.us.api.dto.course.CourseFullInfoDTO;
import com.us.api.dto.exam.ExamFinishedDTO;
import com.us.common.autoconfigure.mq.RabbitMqHelper;
import com.us.common.constants.MqConstants;
import com.us.common.domain.dto.PageDTO;
import com.us.common.domain.query.PageQuery;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.StringUtils;
import com.us.common.utils.UserContext;
import com.us.exam.domain.dto.ExamCommitDTO;
import com.us.exam.domain.dto.ExamDetailDTO;
import com.us.exam.domain.dto.ExamFormDTO;
import com.us.exam.domain.mongo.ExamDetail;
import com.us.exam.domain.mongo.ExamRecord;
import com.us.exam.domain.po.Question;
import com.us.exam.domain.po.QuestionBiz;
import com.us.exam.domain.po.QuestionDetail;
import com.us.exam.domain.vo.ExamQuestionVO;
import com.us.exam.domain.vo.ExamRecordDetailVO;
import com.us.exam.domain.vo.ExamRecordPageVO;
import com.us.exam.domain.vo.ExamVO;
import com.us.exam.mapper.QuestionBizMapper;
import com.us.exam.mapper.QuestionDetailMapper;
import com.us.exam.mapper.QuestionMapper;
import com.us.exam.repository.ExamDetailRepository;
import com.us.exam.repository.ExamRecordRepository;
import com.us.exam.service.IExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考试评测服务实现（day13~14 分组实战）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements IExamService {

    /**
     * 考试类型：练习
     */
    private static final int TYPE_PRACTICE = 1;
    /**
     * 考试类型：考试（每人只能参加一次）
     */
    private static final int TYPE_EXAM = 2;

    private final QuestionMapper questionMapper;

    private final QuestionDetailMapper questionDetailMapper;

    private final QuestionBizMapper questionBizMapper;

    private final ExamRecordRepository examRecordRepository;

    private final ExamDetailRepository examDetailRepository;

    private final CourseClient courseClient;

    private final TradeClient tradeClient;

    private final RabbitMqHelper mqHelper;

    /**
     * 获取试题并开始考试（笔记 3.2.1）
     */
    @Override
    public ExamVO getQuestionsAndBeginExam(ExamFormDTO dto) {
        Long userId = UserContext.getUser();
        // 1.校验用户是否购买了该课程
        Boolean purchased = tradeClient.checkMyLesson(dto.getCourseId());
        if (!Boolean.TRUE.equals(purchased)) {
            throw new BizIllegalException("您还没有购买该课程，无法参加练习或考试");
        }
        // 2.考试只能参加一次（练习可以重复参加）
        if (TYPE_EXAM == dto.getType()) {
            boolean exists = examRecordRepository.existsByUserIdAndSectionIdAndTypeAndFinished(
                    userId, dto.getSectionId(), dto.getType(), true);
            if (exists) {
                throw new BizIllegalException("该考试已经参加过，不能重复参加");
            }
        }
        // 2.1.查询课程信息，并校验该小节确实属于该课程（防止"买了A课却拿B课试卷"的越权取题）
        CourseFullInfoDTO course = courseClient.getCourseInfoById(dto.getCourseId(), true, false);
        if (course == null) {
            throw new BizIllegalException("课程不存在");
        }
        String sectionName = null;
        if (!CollUtils.isEmpty(course.getChapters())) {
            sectionName = findSectionName(course, dto.getSectionId());
            if (sectionName == null) {
                throw new BizIllegalException("小节不属于该课程，无法开始考试");
            }
        }
        // 3.查询小节下的题目
        List<QuestionBiz> bizList = questionBizMapper.selectList(
                Wrappers.<QuestionBiz>lambdaQuery().eq(QuestionBiz::getBizId, dto.getSectionId()));
        if (CollUtils.isEmpty(bizList)) {
            throw new BizIllegalException("该小节没有关联题目");
        }
        List<Long> qIds = bizList.stream().map(QuestionBiz::getQuestionId).collect(Collectors.toList());
        // 4.查询题目与题目详情
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        if (CollUtils.isEmpty(questions)) {
            throw new BizIllegalException("题目不存在");
        }
        Map<Long, QuestionDetail> detailMap = queryQuestionDetailMap(qIds);
        // 5.创建考试记录（记录开始时间，用于计算考试用时）
        ExamRecord record = new ExamRecord();
        record.setId(IdWorker.getId());
        record.setUserId(userId);
        record.setCourseId(dto.getCourseId());
        record.setSectionId(dto.getSectionId());
        record.setType(dto.getType());
        record.setCreateTime(LocalDateTime.now());
        record.setFinished(false);
        record.setQuestionCount(questions.size());
        record.setTotalScore(questions.stream().mapToInt(q -> q.getScore() == null ? 0 : q.getScore()).sum());
        // 5.1.课程名称、小节名称（冗余存储，列表页直接展示）
        record.setCourseName(course.getName());
        record.setSectionName(sectionName);
        examRecordRepository.save(record);
        // 6.组织返回（考试时不返回答案与解析）
        ExamVO vo = new ExamVO();
        vo.setId(record.getId());
        List<ExamQuestionVO> qvos = new ArrayList<>(questions.size());
        for (Question q : questions) {
            ExamQuestionVO qv = BeanUtils.copyBean(q, ExamQuestionVO.class);
            QuestionDetail detail = detailMap.get(q.getId());
            if (detail != null) {
                qv.setOptions(detail.getOptions());
            }
            qvos.add(qv);
        }
        vo.setQuestions(qvos);
        return vo;
    }

    /**
     * 提交考试结果（笔记 3.2.2）：自动批阅 + 保存答题信息 + 通知学习微服务
     */
    @Override
    @Transactional
    public void commitExam(ExamCommitDTO dto) {
        Long userId = UserContext.getUser();
        // 1.校验考试记录是否存在（同时校验归属，防止越权交卷）
        ExamRecord record = examRecordRepository.findByIdAndUserId(dto.getId(), userId)
                .orElseThrow(() -> new BizIllegalException("考试记录不存在"));
        // 2.幂等校验：是否重复提交
        if (Boolean.TRUE.equals(record.getFinished())) {
            throw new BizIllegalException("试卷已经提交，不能重复提交");
        }
        List<ExamDetailDTO> details = dto.getExamDetails() == null ?
                CollUtils.emptyList() : dto.getExamDetails();
        if (CollUtils.isEmpty(details)) {
            // 考试每人只能参加一次，误交白卷会白费考试机会，直接拒绝
            throw new BizIllegalException("答题信息不能为空，无法交卷");
        }
        // 3.查询题目与标准答案
        List<Long> qIds = details.stream().map(ExamDetailDTO::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = CollUtils.isEmpty(qIds) ? Collections.emptyMap() :
                questionMapper.selectBatchIds(qIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, QuestionDetail> detailMap = queryQuestionDetailMap(qIds);
        // 4.自动批阅（目前题目均为客观题，答对得满分）
        int totalScore = 0;
        int correctCount = 0;
        List<ExamDetail> examDetails = new ArrayList<>(details.size());
        for (ExamDetailDTO item : details) {
            Question question = questionMap.get(item.getQuestionId());
            QuestionDetail questionDetail = detailMap.get(item.getQuestionId());
            boolean correct = isCorrect(questionDetail, item.getAnswer());
            int score = correct && question != null && question.getScore() != null ? question.getScore() : 0;
            ExamDetail ed = new ExamDetail();
            ed.setExamId(record.getId());
            ed.setQuestionId(item.getQuestionId());
            // 题目类型以题库为准，不采信前端传值（前端可伪造）
            ed.setQuestionType(question == null ? item.getQuestionType() : question.getType());
            ed.setAnswer(item.getAnswer());
            ed.setCorrect(correct);
            ed.setScore(score);
            ed.setComment(correct ? "回答正确" : "回答错误");
            examDetails.add(ed);
            totalScore += score;
            if (correct) {
                correctCount++;
            }
        }
        // 5.保存答题详情
        examDetailRepository.deleteByExamId(record.getId());
        examDetailRepository.saveAll(examDetails);
        // 6.更新题目的作答统计（回答次数、正确次数），方便后续统计
        updateQuestionStatistics(examDetails);
        // 7.更新考试记录
        record.setScore(totalScore);
        record.setCorrectCount(correctCount);
        record.setFinished(true);
        record.setCommitTime(LocalDateTime.now());
        record.setDuration((int) Duration.between(record.getCreateTime(), record.getCommitTime()).getSeconds());
        examRecordRepository.save(record);
        // 8.通知学习微服务：考试小节属于课程学习的一部分，需要新增学习记录并加积分
        ExamFinishedDTO message = new ExamFinishedDTO();
        message.setUserId(userId);
        message.setCourseId(record.getCourseId());
        message.setSectionId(record.getSectionId());
        message.setScore(totalScore);
        message.setDuration(record.getDuration());
        message.setFinishTime(record.getCommitTime());
        // 事务提交后再发送，避免本地事务（题目统计更新）回滚但消息已经发出
        sendAfterCommit(message);
    }

    /**
     * 事务提交后发送考试完成消息（没有事务时直接发送）
     */
    private void sendAfterCommit(ExamFinishedDTO message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                            MqConstants.Key.EXAM_FINISHED, message);
                }
            });
        } else {
            mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.EXAM_FINISHED, message);
        }
    }

    /**
     * 分页查询我的考试记录（笔记 3.2.3）
     */
    @Override
    public PageDTO<ExamRecordPageVO> queryExamRecordPage(PageQuery query) {
        Long userId = UserContext.getUser();
        // 1.分页查询（MongoDB，默认按提交时间倒序）
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ?
                PageQuery.DEFAULT_PAGE_SIZE : query.getPageSize();
        PageRequest pageRequest = PageRequest.of(pageNo - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "commitTime"));
        Page<ExamRecord> page = examRecordRepository.findByUserIdAndFinished(userId, true, pageRequest);
        // 2.转换VO
        List<ExamRecordPageVO> list = BeanUtils.copyList(page.getContent(), ExamRecordPageVO.class);
        return new PageDTO<>(page.getTotalElements(), (long) page.getTotalPages(), list);
    }

    /**
     * 查询考试记录详情（笔记 3.2.4）
     */
    @Override
    public List<ExamRecordDetailVO> queryExamRecordDetail(Long id) {
        Long userId = UserContext.getUser();
        // 1.校验考试记录归属
        examRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BizIllegalException("考试记录不存在"));
        // 2.查询答题详情
        List<ExamDetail> details = examDetailRepository.findByExamId(id);
        if (CollUtils.isEmpty(details)) {
            return CollUtils.emptyList();
        }
        // 3.查询题目与题目详情（含正确答案、解析）
        List<Long> qIds = details.stream().map(ExamDetail::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<Long, QuestionDetail> detailMap = queryQuestionDetailMap(qIds);
        // 4.组装结果
        List<ExamRecordDetailVO> list = new ArrayList<>(details.size());
        for (ExamDetail ed : details) {
            ExamRecordDetailVO vo = new ExamRecordDetailVO();
            vo.setAnswer(ed.getAnswer());
            vo.setComment(ed.getComment());
            vo.setCorrect(ed.getCorrect());
            vo.setScore(ed.getScore());
            Question question = questionMap.get(ed.getQuestionId());
            if (question != null) {
                ExamQuestionVO qv = BeanUtils.copyBean(question, ExamQuestionVO.class);
                QuestionDetail qd = detailMap.get(ed.getQuestionId());
                if (qd != null) {
                    qv.setOptions(qd.getOptions());
                    qv.setAnswer(qd.getAnswer());
                    qv.setAnalysis(qd.getAnalysis());
                }
                vo.setQuestion(qv);
            }
            list.add(vo);
        }
        return list;
    }

    /**
     * 查询题目详情（key：题目id）
     */
    private Map<Long, QuestionDetail> queryQuestionDetailMap(List<Long> qIds) {
        if (CollUtils.isEmpty(qIds)) {
            return Collections.emptyMap();
        }
        List<QuestionDetail> details = questionDetailMapper.selectBatchIds(qIds);
        if (CollUtils.isEmpty(details)) {
            return Collections.emptyMap();
        }
        return details.stream().collect(Collectors.toMap(QuestionDetail::getId, d -> d));
    }

    /**
     * 判断学员答案是否正确（客观题，多选答案忽略顺序与分隔符差异）
     */
    private boolean isCorrect(QuestionDetail questionDetail, String answer) {
        if (questionDetail == null || StringUtils.isBlank(questionDetail.getAnswer()) || StringUtils.isBlank(answer)) {
            return false;
        }
        return splitAnswer(questionDetail.getAnswer()).equals(splitAnswer(answer));
    }

    /**
     * 把答案字符串（如"1,2,3"、"1，2 3"）解析为集合
     */
    private Set<String> splitAnswer(String answer) {
        return Arrays.stream(answer.split("[,，\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 更新题目的作答统计：回答次数+1，答对的题目正确次数+1
     */
    private void updateQuestionStatistics(List<ExamDetail> examDetails) {
        if (CollUtils.isEmpty(examDetails)) {
            return;
        }
        List<Long> allIds = examDetails.stream().map(ExamDetail::getQuestionId).collect(Collectors.toList());
        // 用IFNULL兜底：历史数据该列可能为NULL，NULL+1仍是NULL，统计会永远不涨
        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .setSql("answer_times = IFNULL(answer_times, 0) + 1")
                .in(Question::getId, allIds));
        List<Long> correctIds = examDetails.stream()
                .filter(ed -> Boolean.TRUE.equals(ed.getCorrect()))
                .map(ExamDetail::getQuestionId)
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(correctIds)) {
            return;
        }
        questionMapper.update(null, Wrappers.<Question>lambdaUpdate()
                .setSql("correct_times = IFNULL(correct_times, 0) + 1")
                .in(Question::getId, correctIds));
    }

    /**
     * 从课程目录树中查找小节名称
     */
    private String findSectionName(CourseFullInfoDTO course, Long sectionId) {
        if (course == null || CollUtils.isEmpty(course.getChapters()) || sectionId == null) {
            return null;
        }
        for (CatalogueDTO chapter : course.getChapters()) {
            // 小节也可能直接就是章（考试小节挂在章下面）
            if (sectionId.equals(chapter.getId())) {
                return chapter.getName();
            }
            if (CollUtils.isEmpty(chapter.getSections())) {
                continue;
            }
            for (CatalogueDTO section : chapter.getSections()) {
                if (sectionId.equals(section.getId())) {
                    return section.getName();
                }
            }
        }
        return null;
    }
}
