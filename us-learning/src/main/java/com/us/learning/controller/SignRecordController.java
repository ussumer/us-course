package com.us.learning.controller;

import com.us.learning.domain.vo.SignRecordVO;
import com.us.learning.domain.vo.SignResultVO;
import com.us.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "签到相关接口")
@RestController
@RequestMapping("sign-records")
@RequiredArgsConstructor
public class SignRecordController {

    private final ISignRecordService recordService;

    @PostMapping
    @ApiOperation("签到功能接口")
    public SignResultVO addSignRecords(){
        return recordService.addSignRecords();
    }

    /**
     * 查询本月签到记录（day07 练习4.1，由学员实现）
     */
    @GetMapping
    @ApiOperation("查询本月签到记录")
    public SignRecordVO querySignRecords(){
        return recordService.querySignRecords();
    }
}