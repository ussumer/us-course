package com.us.learning.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.dto.ReplyDTO;
import com.us.learning.domain.query.ReplyPageQuery;
import com.us.learning.domain.vo.ReplyVO;
import com.us.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的回答或评论 前端控制器（用户端）
 * </p>
 */
@RestController
@RequestMapping("/replies")
@Api(tags = "互动问答相关接口-回答与评论")
@RequiredArgsConstructor
public class InteractionReplyController {

    private final IInteractionReplyService replyService;

    /**
     * 新增回答或评论（day05 练习4.1，业务逻辑由学员实现）
     */
    @ApiOperation("回答问题、评论回答或评论")
    @PostMapping
    public void saveReply(@Valid @RequestBody ReplyDTO replyDTO){
        replyService.saveReply(replyDTO);
    }

    /**
     * 分页查询回答或评论列表
     * 回答列表传questionId；评论列表传answerId
     */
    @ApiOperation("分页查询回答或评论列表")
    @GetMapping("page")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query){
        return replyService.queryReplyPage(query);
    }
}