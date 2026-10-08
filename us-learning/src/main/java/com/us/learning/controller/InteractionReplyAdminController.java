package com.us.learning.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.query.ReplyPageQuery;
import com.us.learning.domain.vo.ReplyVO;
import com.us.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动提问的回答或评论 前端控制器（管理端）
 * </p>
 */
@RestController
@RequestMapping("/admin/replies")
@Api(tags = "互动问答相关接口-回答与评论-管理端")
@RequiredArgsConstructor
public class InteractionReplyAdminController {

    private final IInteractionReplyService replyService;

    /**
     * 管理端分页查询回答或评论（统计包含隐藏、无视匿名、返回hidden字段）
     */
    @ApiOperation("管理端分页查询回答或评论")
    @GetMapping("page")
    public PageDTO<ReplyVO> queryReplyPageAdmin(ReplyPageQuery query){
        return replyService.queryReplyPageAdmin(query);
    }

    /**
     * 管理端隐藏或显示指定回答或评论（隐藏回答时连带其下所有评论）
     */
    @ApiOperation("管理端隐藏或显示回答或评论")
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenReply(
            @PathVariable("id") Long id,
            @PathVariable("hidden") Boolean hidden){
        replyService.hiddenReply(id, hidden);
    }
}