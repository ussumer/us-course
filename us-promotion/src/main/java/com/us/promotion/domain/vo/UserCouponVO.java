package com.us.promotion.domain.vo;

import com.us.promotion.enums.DiscountType;
import com.us.promotion.enums.UserCouponStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的优惠券 VO（day10 练习 4.1：分页查询我的优惠券）
 */
@Data
@ApiModel(description = "我的优惠券信息")
public class UserCouponVO {

    @ApiModelProperty("用户券id")
    private Long id;

    @ApiModelProperty("优惠券名称")
    private String name;

    @ApiModelProperty("是否限定使用范围")
    private Boolean specific;

    @ApiModelProperty("优惠券类型，1：每满减，2：折扣，3：无门槛，4：普通满减")
    private DiscountType discountType;
    @ApiModelProperty("折扣门槛，0代表无门槛")
    private Integer thresholdAmount;
    @ApiModelProperty("折扣值")
    private Integer discountValue;
    @ApiModelProperty("最大优惠金额")
    private Integer maxDiscountAmount;

    @ApiModelProperty("有效天数")
    private Integer termDays;
    @ApiModelProperty("优惠券有效期结束时间")
    private LocalDateTime termEndTime;

    @ApiModelProperty("用户券状态，1：未使用，2：已使用，3：已过期")
    private UserCouponStatus status;
}
