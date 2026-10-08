package com.us.learning.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.learning.domain.dto.QuestionFormDTO;
import com.us.learning.domain.query.QuestionPageQuery;
import com.us.learning.domain.vo.QuestionVO;
import com.us.learning.service.IInteractionQuestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器（用户端）
 * </p>
 */
@RestController
@RequestMapping("/questions")
@Api(tags = "互动问答相关接口")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    /**
     * 新增提问（课程详情页 / 视频学习页）
     */
    @ApiOperation("新增提问")
    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO questionDTO){
        questionService.saveQuestion(questionDTO);
    }

    /**
     * 修改我的问题（day05 练习3.2）：只能修改标题、描述、是否匿名
     */
    @ApiOperation("修改我的问题")
    @PutMapping("/{id}")
    public void updateQuestion(
            @ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id,
            @RequestBody QuestionFormDTO questionDTO){
        questionService.updateQuestion(id, questionDTO);
    }

    /**
     * 删除我的问题（day05 练习3.5）：连带删除问题下的回答和评论
     */
    @ApiOperation("删除我的问题")
    @DeleteMapping("/{id}")
    public void deleteQuestion(
            @ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id){
        questionService.deleteQuestion(id);
    }

    /**
     * 分页查询互动问题（课程详情页问答选项卡）
     */
    @ApiOperation("分页查询互动问题")
    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
        return questionService.queryQuestionPage(query);
    }

    /**
     * 根据id查询问题详情
     */
    @ApiOperation("根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id){
        return questionService.queryQuestionById(id);
    }
}