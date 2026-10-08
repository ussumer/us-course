package com.us.exam.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.common.domain.query.PageQuery;
import com.us.exam.domain.dto.ExamCommitDTO;
import com.us.exam.domain.dto.ExamFormDTO;
import com.us.exam.domain.vo.ExamRecordDetailVO;
import com.us.exam.domain.vo.ExamRecordPageVO;
import com.us.exam.domain.vo.ExamVO;
import com.us.exam.service.IExamService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 考试评测 前端控制器（day13~14 分组实战）
 * </p>
 */
@RestController
@RequestMapping("/exams")
@Api(tags = "考试相关接口")
@RequiredArgsConstructor
public class ExamController {

    private final IExamService examService;

    @ApiOperation("获取试题并开始考试")
    @PostMapping
    public ExamVO getQuestionsAndBeginExam(@RequestBody @Valid ExamFormDTO dto) {
        return examService.getQuestionsAndBeginExam(dto);
    }

    @ApiOperation("提交考试结果")
    @PostMapping("/details")
    public void commitExam(@RequestBody @Valid ExamCommitDTO dto) {
        examService.commitExam(dto);
    }

    @ApiOperation("分页查询我的考试记录")
    @GetMapping("/page")
    public PageDTO<ExamRecordPageVO> queryExamRecordPage(PageQuery query) {
        return examService.queryExamRecordPage(query);
    }

    @ApiOperation("查询考试记录详情")
    @GetMapping("/{id}")
    public List<ExamRecordDetailVO> queryExamRecordDetail(@ApiParam("考试记录id") @PathVariable("id") Long id) {
        return examService.queryExamRecordDetail(id);
    }
}
