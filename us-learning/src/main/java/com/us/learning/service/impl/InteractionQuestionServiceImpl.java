package com.us.learning.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.api.cache.CategoryCache;
import com.us.api.client.course.CatalogueClient;
import com.us.api.client.course.CourseClient;
import com.us.api.client.search.SearchClient;
import com.us.api.client.user.UserClient;
import com.us.api.dto.course.CataSimpleInfoDTO;
import com.us.api.dto.course.CourseFullInfoDTO;
import com.us.api.dto.course.CourseSimpleInfoDTO;
import com.us.api.dto.user.UserDTO;
import com.us.common.domain.dto.PageDTO;
import com.us.common.exceptions.BadRequestException;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.StringUtils;
import com.us.common.utils.UserContext;
import com.us.learning.domain.dto.QuestionFormDTO;
import com.us.learning.domain.po.InteractionQuestion;
import com.us.learning.domain.po.InteractionReply;
import com.us.learning.domain.query.QuestionAdminPageQuery;
import com.us.learning.domain.query.QuestionPageQuery;
import com.us.learning.domain.vo.QuestionAdminVO;
import com.us.learning.domain.vo.QuestionVO;
import com.us.learning.enums.QuestionStatus;
import com.us.learning.mapper.InteractionQuestionMapper;
import com.us.learning.mapper.InteractionReplyMapper;
import com.us.learning.service.IInteractionQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final SearchClient searchClient;
    private final CategoryCache categoryCache;

    /**
     * 新增提问（day05 笔记 3.1.3）
     */
    @Override
    @Transactional
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.数据转换
        InteractionQuestion question = BeanUtils.toBean(questionDTO, InteractionQuestion.class);
        // 3.补充数据（其余字段取默认值：answerTimes=0、hidden=false、status=未查看）
        question.setUserId(userId);
        // 4.保存问题
        save(question);
    }

    /**
     * 用户端分页查询互动问题（day05 笔记 3.1.5）
     */
    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id和小节id不能都为空
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 2.分页查询（排除大字段description；只查未被隐藏的问题；默认按提问时间倒序）
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
                // 增强：笔记原版eq(query.getOnlyMine(), ...)在onlyMine为null时拆箱NPE，这里用TRUE.equals判等
                .eq(Boolean.TRUE.equals(query.getOnlyMine()), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()) { // 只查询非匿名的问题
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 3.2.根据id查询最近一次回答
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){ // 匿名用户不做查询
                    userIds.add(reply.getUserId());
                }
            }
        }
        // 3.3.根据id查询用户信息（提问者、回答者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            // 4.1.将PO转为VO
            QuestionVO vo = BeanUtils.copyBean(r, QuestionVO.class);
            vo.setUserId(null);
            voList.add(vo);
            // 4.2.封装提问者信息（匿名则不展示用户信息）
            if(!r.getAnonymity()){
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }
            // 4.3.封装最近一次回答的信息
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if(!reply.getAnonymity()){// 匿名用户直接忽略
                    UserDTO user = userMap.get(reply.getUserId());
                    if (user != null) {
                        vo.setLatestReplyUser(user.getName());
                    }
                }
            }
        }
        return PageDTO.of(page, voList);
    }

    /**
     * 根据id查询问题详情（day05 笔记 3.4.3）
     */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1.根据id查询数据
        InteractionQuestion question = getById(id);
        // 2.数据校验：没有数据或者是被隐藏了
        if(question == null || question.getHidden()){
            return null;
        }
        // 3.查询提问者信息（匿名则不查）
        UserDTO user = null;
        if(!question.getAnonymity()){
            user = userClient.queryUserById(question.getUserId());
        }
        // 4.封装VO
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    /**
     * 管理端分页查询互动问题（day05 笔记 3.6.5.5）
     */
    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        // 1.处理课程名称，得到课程id（课程名称在ES中，走SearchClient全文检索）
        List<Long> courseIds = null;
        if (StringUtils.isNotBlank(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                // 没搜到任何课程，直接返回空页
                return PageDTO.empty(0L, 0L);
            }
        }
        // 2.分页查询
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.准备VO需要的数据：用户数据、课程数据、章节数据
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();
        // 3.1.获取各种数据的id集合
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }
        // 3.2.根据id查询用户（管理端无视匿名，全部查询）
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>(users.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        // 3.3.根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }
        // 3.4.根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        // 4.封装VO
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            // 4.1.将PO转VO，属性拷贝
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            // 4.2.用户信息（管理端无视匿名）
            UserDTO user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
                vo.setUserIcon(user.getIcon());
            }
            // 4.3.课程信息以及分类信息（分类走Caffeine本地缓存）
            CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            }
            // 4.4.章节信息
            vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
            vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
        }
        return PageDTO.of(page, voList);
    }

    /**
     * 修改我的问题（day05 练习3.2）：只允许修改标题、描述、是否匿名
     */
    @Override
    @Transactional
    public void updateQuestion(Long id, QuestionFormDTO questionDTO) {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        // 2.查询问题并校验归属
        InteractionQuestion q = getById(id);
        if (q == null || !q.getUserId().equals(userId)) {
            throw new BadRequestException("问题不存在或无权修改");
        }
        // 3.只更新表单允许修改的三个字段（课程、章节关联不允许改）
        InteractionQuestion question = new InteractionQuestion();
        question.setId(id);
        question.setTitle(questionDTO.getTitle());
        question.setDescription(questionDTO.getDescription());
        question.setAnonymity(questionDTO.getAnonymity());
        updateById(question);
    }

    /**
     * 删除我的问题（day05 练习3.5）：连带删除问题下的回答和评论
     */
    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        // 1.获取当前用户
        Long userId = UserContext.getUser();
        // 2.查询问题并校验归属
        InteractionQuestion q = getById(id);
        if (q == null || !q.getUserId().equals(userId)) {
            throw new BadRequestException("问题不存在或无权删除");
        }
        // 3.删除问题
        removeById(id);
        // 4.删除问题下的回答及评论
        replyMapper.delete(Wrappers.<InteractionReply>lambdaQuery()
                .eq(InteractionReply::getQuestionId, id));
    }

    /**
     * 管理端隐藏或显示问题（day05 练习3.7）
     */
    @Override
    public void hiddenQuestion(Long id, Boolean hidden) {
        lambdaUpdate()
                .set(InteractionQuestion::getHidden, hidden)
                .eq(InteractionQuestion::getId, id)
                .update();
    }

    /**
     * 管理端根据id查询问题详情（day05 练习3.8）：多返回课程负责老师，并把问题标记为已查看
     */
    @Override
    public QuestionAdminVO queryQuestionByIdAdmin(Long id) {
        // 1.查询问题
        InteractionQuestion q = getById(id);
        if (q == null) {
            return null;
        }
        // 2.管理员点击查看，标记问题状态为已查看
        lambdaUpdate()
                .set(InteractionQuestion::getStatus, QuestionStatus.CHECKED)
                .eq(InteractionQuestion::getId, id)
                .update();
        // 3.查询提问者信息（管理端无视匿名）
        QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
        UserDTO user = userClient.queryUserById(q.getUserId());
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        // 4.查询课程信息（withTeachers=true 拿到负责老师id）
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(q.getCourseId(), false, true);
        if (cInfo != null) {
            vo.setCourseName(cInfo.getName());
            vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            // 5.课程负责老师（取第一位）
            if (CollUtils.isNotEmpty(cInfo.getTeacherIds())) {
                List<UserDTO> teachers = userClient.queryUserByIds(cInfo.getTeacherIds());
                if (CollUtils.isNotEmpty(teachers)) {
                    vo.setTeacherName(teachers.get(0).getName());
                }
            }
        }
        // 6.章节名称
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(
                CollUtils.newArrayList(q.getChapterId(), q.getSectionId()));
        Map<Long, String> cataMap = catas.stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
        vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
        return vo;
    }
}
