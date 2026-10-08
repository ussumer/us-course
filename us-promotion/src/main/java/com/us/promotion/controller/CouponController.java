package com.us.promotion.controller;

import com.us.common.domain.dto.PageDTO;
import com.us.promotion.domain.dto.CouponFormDTO;
import com.us.promotion.domain.dto.CouponIssueFormDTO;
import com.us.promotion.domain.query.CouponQuery;
import com.us.promotion.domain.vo.CouponDetailVO;
import com.us.promotion.domain.vo.CouponPageVO;
import com.us.promotion.domain.vo.CouponVO;
import com.us.promotion.service.ICouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")
@Api(tags = "优惠券相关接口")
public class CouponController {

    private final ICouponService couponService;

    @ApiOperation("新增优惠券接口")
    @PostMapping
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto) {
        couponService.saveCoupon(dto);
    }

    @ApiOperation("分页查询优惠券接口")
    @GetMapping("/page")
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        return couponService.queryCouponByPage(query);
    }

    @ApiOperation("查询发放中的优惠券列表")
    @GetMapping("/list")
    public List<CouponVO> queryIssuingCoupons() {
        return couponService.queryIssuingCoupons();
    }

    @ApiOperation("发放优惠券接口")
    @PutMapping("/{id}/issue")
    public void beginIssue(@ApiParam("优惠券id") @PathVariable("id") Long id,
                           @RequestBody @Valid CouponIssueFormDTO dto) {
        dto.setId(id);
        couponService.beginIssue(dto);
    }

    @ApiOperation("修改优惠券接口")
    @PutMapping("/{id}")
    public void updateCoupon(@ApiParam("优惠券id") @PathVariable("id") Long id,
                             @RequestBody @Valid CouponFormDTO dto) {
        dto.setId(id);
        couponService.updateCoupon(dto);
    }

    @ApiOperation("删除优惠券接口")
    @DeleteMapping("/{id}")
    public void deleteCouponById(@ApiParam("优惠券id") @PathVariable("id") Long id) {
        couponService.deleteCouponById(id);
    }

    @ApiOperation("根据id查询优惠券接口")
    @GetMapping("/{id}")
    public CouponDetailVO queryCouponById(@ApiParam("优惠券id") @PathVariable("id") Long id) {
        return couponService.queryCouponById(id);
    }

    @ApiOperation("暂停发放优惠券接口")
    @PutMapping("/{id}/pause")
    public void pauseCoupon(@ApiParam("优惠券id") @PathVariable("id") Long id) {
        couponService.pauseCoupon(id);
    }
}
