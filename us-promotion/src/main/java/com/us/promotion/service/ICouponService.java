package com.us.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.promotion.domain.dto.CouponFormDTO;
import com.us.promotion.domain.dto.CouponIssueFormDTO;
import com.us.promotion.domain.po.Coupon;
import com.us.promotion.domain.query.CouponQuery;
import com.us.promotion.domain.vo.CouponDetailVO;
import com.us.promotion.domain.vo.CouponPageVO;
import com.us.promotion.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 */
public interface ICouponService extends IService<Coupon> {

    /**
     * 新增优惠券（笔记 2.1）
     *
     * @param dto 优惠券表单数据
     */
    void saveCoupon(CouponFormDTO dto);

    /**
     * 分页查询优惠券（笔记 2.2）
     *
     * @param query 分页与过滤条件
     * @return 优惠券分页结果
     */
    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);

    /**
     * 查询发放中的优惠券（day10 笔记 2.1）
     *
     * @return 发放中且手动领取的优惠券列表（含是否可领、是否已领取标记）
     */
    List<CouponVO> queryIssuingCoupons();

    /**
     * 发放优惠券（笔记 3.1）
     *
     * @param dto 发放表单数据
     */
    void beginIssue(CouponIssueFormDTO dto);

    /**
     * 修改优惠券（笔记 4.1 练习：只有待发放状态可以修改）
     *
     * @param dto 优惠券表单数据（必须携带id）
     */
    void updateCoupon(CouponFormDTO dto);

    /**
     * 删除优惠券（笔记 4.2 练习：只有待发放状态可以删除）
     *
     * @param id 优惠券id
     */
    void deleteCouponById(Long id);

    /**
     * 根据id查询优惠券详情（笔记 4.3 练习）
     *
     * @param id 优惠券id
     * @return 优惠券详情，含使用范围
     */
    CouponDetailVO queryCouponById(Long id);

    /**
     * 暂停发放优惠券（笔记 4.6 练习 + day11 2.2.4：同时清理Redis缓存）
     *
     * @param id 优惠券id
     */
    void pauseCoupon(Long id);

    /**
     * 定时开始发放优惠券（day09 练习 4.4 + day11 2.2.3：同时预热Redis缓存）
     *
     * @param shardIndex 当前分片序号
     * @param shardTotal 分片总数
     */
    void beginIssueCoupon(int shardIndex, int shardTotal);

    /**
     * 定时结束发放优惠券（day09 练习 4.5）
     *
     * @param shardIndex 当前分片序号
     * @param shardTotal 分片总数
     */
    void finishIssueCoupon(int shardIndex, int shardTotal);
}
