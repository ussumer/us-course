package com.us.promotion.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.promotion.domain.query.CodeQuery;
import com.us.promotion.domain.vo.CodeVO;
import com.us.promotion.service.IExchangeCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/codes")
@Api(tags = "兑换码相关接口")
public class ExchangeCodeController {

    private final IExchangeCodeService exchangeCodeService;

    @ApiOperation("分页查询兑换码接口")
    @GetMapping("/page")
    public PageDTO<CodeVO> queryCodeByPage(@Valid CodeQuery query) {
        return exchangeCodeService.queryCodeByPage(query);
    }
}
