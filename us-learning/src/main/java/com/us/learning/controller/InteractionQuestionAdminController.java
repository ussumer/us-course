package com.us.learning.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.query.QuestionAdminPageQuery;
import com.us.learning.domain.vo.QuestionAdminVO;
import com.us.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 互动提问的问题表 前端控制器（管理端）
 * </p>
 */
@RestController
@RequestMapping("/admin/questions")
@Api(tags = "互动问答相关接口-管理端")
@RequiredArgsConstructor
public class InteractionQuestionAdminController {

    private final IInteractionQuestionService questionService;

    /**
     * 管理端分页查询互动问题（支持课程名称关键字、状态、时间区间过滤）
     */
    @ApiOperation("管理端分页查询互动问题")
    @GetMapping("page")
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query){
        return questionService.queryQuestionPageAdmin(query);
    }

    /**
     * 管理端根据id查询问题详情（day05 练习3.8）：会把问题标记为已查看
     */
    @ApiOperation("管理端根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionAdminVO queryQuestionByIdAdmin(
            @ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id){
        return questionService.queryQuestionByIdAdmin(id);
    }

    /**
     * 管理端隐藏或显示指定问题（day05 练习3.7）
     */
    @ApiOperation("管理端隐藏或显示问题")
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenQuestion(
            @ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id,
            @PathVariable("hidden") Boolean hidden){
        questionService.hiddenQuestion(id, hidden);
    }
}