package com.us.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.dto.ReplyDTO;
import com.us.learning.domain.po.InteractionReply;
import com.us.learning.domain.query.ReplyPageQuery;
import com.us.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动提问的回答或评论 服务类
 * </p>
 */
public interface IInteractionReplyService extends IService<InteractionReply> {

    /**
     * 新增回答或评论（day05 练习4.1，业务逻辑由学员实现）
     * @param replyDTO 回答/评论表单
     */
    void saveReply(ReplyDTO replyDTO);

    /**
     * 用户端分页查询回答或评论
     * @param query 查询条件（问题id或回答id至少一个）
     * @return 回答/评论分页结果
     */
    PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query);

    /**
     * 管理端分页查询回答或评论（统计包含隐藏、无视匿名，返回hidden字段）
     * @param query 查询条件（问题id或回答id至少一个）
     * @return 回答/评论分页结果
     */
    PageDTO<ReplyVO> queryReplyPageAdmin(ReplyPageQuery query);

    /**
     * 管理端隐藏或显示回答或评论（隐藏回答时连带其下所有评论）
     * @param id 回答或评论id
     * @param hidden 是否隐藏
     */
    void hiddenReply(Long id, Boolean hidden);
}