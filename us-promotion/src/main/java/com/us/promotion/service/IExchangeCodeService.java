package com.us.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.promotion.domain.po.Coupon;
import com.us.promotion.domain.po.ExchangeCode;
import com.us.promotion.domain.query.CodeQuery;
import com.us.promotion.domain.vo.CodeVO;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {

    /**
     * 异步生成兑换码（笔记 3.3.2）
     *
     * @param coupon 优惠券
     */
    void asyncGenerateCode(Coupon coupon);

    /**
     * 分页查询兑换码（笔记 4.7 练习）
     *
     * @param query 查询条件：优惠券id、兑换码状态、分页参数
     * @return 兑换码分页结果
     */
    PageDTO<CodeVO> queryCodeByPage(CodeQuery query);

    /**
     * 基于BitMap标记兑换码的兑换状态（day10 笔记 2.3.2）
     *
     * @param serialNum 兑换码序列号
     * @param mark      true-标记已兑换，false-重置为未兑换
     * @return 修改前的bit值，true表示之前已经兑换过
     */
    boolean updateExchangeMark(long serialNum, boolean mark);

    /**
     * 根据兑换码序列号查询对应的优惠券id（day11 练习3.1）
     *
     * @param serialNum 兑换码序列号
     * @return 优惠券id，查不到返回null
     */
    Long exchangeTargetId(long serialNum);
}
