package com.us.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.promotion.domain.po.CouponScope;
import com.us.promotion.mapper.CouponScopeMapper;
import com.us.promotion.service.ICouponScopeService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 优惠券作用范围 服务实现类
 * </p>
 */
@Service
public class CouponScopeServiceImpl extends ServiceImpl<CouponScopeMapper, CouponScope> implements ICouponScopeService {
}
