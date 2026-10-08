package com.us.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.us.promotion.domain.po.Coupon;
import com.us.promotion.domain.po.UserCoupon;
import com.us.promotion.enums.UserCouponStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

/**
 * <p>
 * 用户领取优惠券的记录 Mapper 接口
 * </p>
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Insert("INSERT IGNORE INTO coupon_receive_record(request_id, user_id, coupon_id) " +
            "VALUES(#{requestId}, #{userId}, #{couponId})")
    int recordReceive(@Param("requestId") String requestId, @Param("userId") Long userId,
                      @Param("couponId") Long couponId);

    /**
     * 查询用户的可用优惠券（day12 笔记 2.3.1，联合 coupon 表查询）
     * 注意：查询结果中 user_coupon.id 别名为 creater，用于返回用户券id
     *
     * @param userId 用户id
     * @return 优惠券信息（creater字段中携带用户券id）
     */
    List<Coupon> queryMyCoupons(@Param("userId") Long userId);

    /**
     * 根据用户券id集合查询优惠券信息（day12 笔记 3.1.3）
     *
     * @param userCouponIds 用户券id集合
     * @param status        用户券状态
     * @return 优惠券信息（creater字段中携带用户券id）
     */
    List<Coupon> queryCouponByUserCouponIds(
            @Param("userCouponIds") List<Long> userCouponIds,
            @Param("status") UserCouponStatus status, @Param("userId") Long userId);
}
