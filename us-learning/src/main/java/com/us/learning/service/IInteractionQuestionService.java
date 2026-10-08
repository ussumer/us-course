package com.us.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.dto.QuestionFormDTO;
import com.us.learning.domain.po.InteractionQuestion;
import com.us.learning.domain.query.QuestionAdminPageQuery;
import com.us.learning.domain.query.QuestionPageQuery;
import com.us.learning.domain.vo.QuestionAdminVO;
import com.us.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    /**
     * 新增提问
     * @param questionDTO 问题表单
     */
    void saveQuestion(QuestionFormDTO questionDTO);

    /**
     * 用户端分页查询互动问题
     * @param query 查询条件（课程/小节/只看我的 + 分页）
     * @return 问题分页结果（含提问者信息、最近一次回答信息）
     */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);

    /**
     * 根据id查询问题详情（用户端）
     * @param id 问题id
     * @return 问题详情，不存在或已隐藏返回null
     */
    QuestionVO queryQuestionById(Long id);

    /**
     * 管理端分页查询互动问题
     * @param query 查询条件（课程名称关键字/状态/时间区间 + 分页）
     * @return 问题分页结果（含用户、课程、章节、分类信息）
     */
    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);

    /**
     * 修改我的问题（day05 练习3.2）：只允许修改标题、描述、是否匿名
     * @param id 问题id
     * @param questionDTO 表单
     */
    void updateQuestion(Long id, QuestionFormDTO questionDTO);

    /**
     * 删除我的问题（day05 练习3.5）：连带删除问题下的回答和评论
     * @param id 问题id
     */
    void deleteQuestion(Long id);

    /**
     * 管理端隐藏或显示问题（day05 练习3.7）
     * @param id 问题id
     * @param hidden 是否隐藏
     */
    void hiddenQuestion(Long id, Boolean hidden);

    /**
     * 管理端根据id查询问题详情（day05 练习3.8）：并把问题标记为已查看
     * @param id 问题id
     * @return 问题详情（含课程负责老师）
     */
    QuestionAdminVO queryQuestionByIdAdmin(Long id);
}