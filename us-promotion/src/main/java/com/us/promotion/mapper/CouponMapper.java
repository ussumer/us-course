package com.us.promotion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.us.promotion.domain.po.Coupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("UPDATE coupon SET used_num = used_num + #{increment} " +
            "WHERE id = #{couponId} AND used_num + #{increment} >= 0")
    int changeUsedNum(@Param("couponId") Long couponId, @Param("increment") int increment);

    /**
     * 已发放数量 +1（day10 3.1.3：乐观锁思想，库存不足时更新失败返回0）
     *
     * @param couponId 优惠券id
     * @return 影响行数，0表示库存不足
     */
    @Update("UPDATE coupon SET issue_num = issue_num + 1 WHERE id = #{couponId} AND issue_num < total_num")
    int incrIssueNum(@Param("couponId") Long couponId);

    /**
     * 已使用数量增减（day12 笔记 3.2.2 / 3.3.2）
     *
     * @param couponIds 优惠券id集合
     * @param increment 增量，1表示核销，-1表示退还
     * @return 影响行数
     */
    @Update("<script>" +
            "UPDATE coupon SET used_num = used_num + #{increment} WHERE id IN " +
            "<foreach collection='couponIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int incrUsedNum(@Param("couponIds") List<Long> couponIds, @Param("increment") int increment);
}
