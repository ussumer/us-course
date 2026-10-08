package com.us.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.common.domain.dto.PageDTO;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.exceptions.DbException;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.JsonUtils;
import com.us.common.utils.UserContext;
import com.us.promotion.domain.dto.UserCouponDTO;
import com.us.promotion.domain.po.Coupon;
import com.us.promotion.domain.po.ExchangeCode;
import com.us.promotion.domain.po.UserCoupon;
import com.us.promotion.domain.query.UserCouponQuery;
import com.us.promotion.domain.vo.UserCouponVO;
import com.us.promotion.enums.ExchangeCodeStatus;
import com.us.promotion.enums.UserCouponStatus;
import com.us.promotion.mapper.CouponMapper;
import com.us.promotion.mapper.UserCouponMapper;
import com.us.promotion.service.IExchangeCodeService;
import com.us.promotion.service.IUserCouponService;
import com.us.promotion.strategy.discount.DiscountStrategy;
import com.us.promotion.utils.CodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.us.promotion.constants.PromotionConstants.*;

/**
 * <p>
 * 用户领取优惠券的记录，是真正使用的优惠券信息 服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon> implements IUserCouponService {

    /**
     * 领券资格校验的LUA脚本（day11 3.3）
     */
    private static final RedisScript<Long> RECEIVE_COUPON_SCRIPT;
    /**
     * 兑换码兑换资格校验的LUA脚本（day11 3.3）
     */
    private static final RedisScript<Long> EXCHANGE_COUPON_SCRIPT;

    static {
        RECEIVE_COUPON_SCRIPT = RedisScript.of(new ClassPathResource("lua/receive_coupon.lua"), Long.class);
        EXCHANGE_COUPON_SCRIPT = RedisScript.of(new ClassPathResource("lua/exchange_coupon.lua"), Long.class);
    }

    private final CouponMapper couponMapper;

    private final IExchangeCodeService codeService;

    private final StringRedisTemplate redisTemplate;

    public static final String OUTBOX_DATA_KEY = "prs:receive:outbox:data";
    public static final String OUTBOX_PENDING_KEY = "prs:receive:outbox:pending";

    /**
     * 领取优惠券（day11 3.3.6：一次LUA脚本完成全部资格校验，然后发MQ异步领券）
     */
    @Override
    public void receiveCoupon(Long couponId) {
        Long userId = UserContext.getUser();
        UserCouponDTO event = newReceiveEvent(userId, couponId, null);
        Long result = redisTemplate.execute(RECEIVE_COUPON_SCRIPT,
                List.of(COUPON_CACHE_KEY_PREFIX + couponId, USER_COUPON_CACHE_KEY_PREFIX + couponId,
                        OUTBOX_DATA_KEY, OUTBOX_PENDING_KEY),
                userId.toString(), event.getRequestId(), JsonUtils.toJsonStr(event),
                String.valueOf(System.currentTimeMillis()));
        if (result == null) {
            throw new BizIllegalException("领取失败，请稍后重试");
        }
        if (result != 0) {
            throw new BizIllegalException(RECEIVE_COUPON_ERROR_MSG[result.intValue() - 1]);
        }
        // Lua 同时保存库存预扣和待投递事件。MQ 不可用时事件仍在 Redis 中等待重试。
    }

    /**
     * 兑换码兑换优惠券（day11 练习3.1：LUA脚本校验 + MQ异步领券）
     */
    @Override
    public void exchangeCoupon(String code) {
        long serialNum = CodeUtil.parseCode(code);
        Long userId = UserContext.getUser();
        ExchangeCode exchangeCode = codeService.getById(serialNum);
        if (exchangeCode == null || !code.equals(exchangeCode.getCode())) {
            throw new BizIllegalException("兑换码不存在");
        }
        UserCouponDTO event = newReceiveEvent(userId, exchangeCode.getExchangeTargetId(), (int) serialNum);
        Long result = redisTemplate.execute(EXCHANGE_COUPON_SCRIPT,
                List.of(COUPON_CODE_MAP_KEY, COUPON_CACHE_KEY_PREFIX + event.getCouponId(),
                        USER_COUPON_CACHE_KEY_PREFIX + event.getCouponId(), OUTBOX_DATA_KEY, OUTBOX_PENDING_KEY),
                String.valueOf(serialNum), userId.toString(), event.getRequestId(),
                JsonUtils.toJsonStr(event), String.valueOf(System.currentTimeMillis()));
        if (result == null) {
            throw new BizIllegalException("兑换失败，请稍后重试");
        }
        if (result != 0) {
            throw new BizIllegalException("兑换码已使用、尚未发放、已过期、库存不足或达到领取上限");
        }
    }

    /**
     * 领券并生成用户券（day11 2.4.3：MQ消费入口，此处已无需加锁和Redis校验）
     */
    @Transactional
    @Override
    public void checkAndCreateUserCoupon(UserCouponDTO uc) {
        if (uc.getRequestId() == null || uc.getUserId() == null || uc.getCouponId() == null) {
            throw new BizIllegalException("领取消息不完整");
        }
        // 唯一主键和业务写入处于同一事务；并发重复消费只有一次成功。
        if (baseMapper.recordReceive(uc.getRequestId(), uc.getUserId(), uc.getCouponId()) == 0) {
            return;
        }
        Coupon coupon = couponMapper.selectById(uc.getCouponId());
        if (coupon == null) {
            throw new BizIllegalException("优惠券不存在！");
        }
        if (uc.getSerialNum() != null) {
            boolean updated = codeService.lambdaUpdate()
                    .set(ExchangeCode::getUserId, uc.getUserId())
                    .set(ExchangeCode::getStatus, ExchangeCodeStatus.USED)
                    .eq(ExchangeCode::getId, uc.getSerialNum())
                    .eq(ExchangeCode::getExchangeTargetId, uc.getCouponId())
                    .eq(ExchangeCode::getStatus, ExchangeCodeStatus.UNUSED).update();
            if (!updated) {
                throw new BizIllegalException("兑换码已兑换，消息需核对");
            }
        }
        if (couponMapper.incrIssueNum(coupon.getId()) != 1) {
            throw new BizIllegalException("优惠券库存不足！");
        }
        saveUserCoupon(coupon, uc.getUserId());
    }

    /**
     * 核销优惠券（day12 笔记 3.2.2）
     */
    @Override
    @Transactional
    public void writeOffCoupon(List<Long> userCouponIds) {
        changeCouponStatus(userCouponIds, false);
    }

    /**
     * 退还优惠券（day12 笔记 3.3.2）
     */
    @Override
    @Transactional
    public void refundCoupon(List<Long> userCouponIds) {
        changeCouponStatus(userCouponIds, true);
    }

    private void changeCouponStatus(List<Long> ids, boolean refund) {
        if (CollUtils.isEmpty(ids)) {
            return;
        }
        Long userId = UserContext.getUser();
        List<UserCoupon> coupons = listByIds(ids.stream().distinct().sorted().collect(Collectors.toList()));
        coupons.sort(java.util.Comparator.comparing(UserCoupon::getId));
        Map<Long, Integer> changes = new java.util.TreeMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (UserCoupon coupon : coupons) {
            if (!Objects.equals(userId, coupon.getUserId())) {
                throw new BizIllegalException("不能操作其他用户的优惠券");
            }
            UserCouponStatus expected = refund ? UserCouponStatus.USED : UserCouponStatus.UNUSED;
            if (coupon.getStatus() != expected) {
                continue;
            }
            if (!refund && (now.isBefore(coupon.getTermBeginTime()) || now.isAfter(coupon.getTermEndTime()))) {
                continue;
            }
            UserCouponStatus target = refund
                    ? (now.isAfter(coupon.getTermEndTime()) ? UserCouponStatus.EXPIRED : UserCouponStatus.UNUSED)
                    : UserCouponStatus.USED;
            boolean changed = lambdaUpdate()
                    .eq(UserCoupon::getId, coupon.getId()).eq(UserCoupon::getUserId, userId)
                    .eq(UserCoupon::getStatus, expected)
                    .set(UserCoupon::getStatus, target)
                    .set(UserCoupon::getUsedTime, refund ? null : now).update();
            if (changed) {
                changes.merge(coupon.getCouponId(), refund ? -1 : 1, Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> entry : changes.entrySet()) {
            if (couponMapper.changeUsedNum(entry.getKey(), entry.getValue()) != 1) {
                throw new DbException("更新优惠券使用数量失败，事务已回滚");
            }
        }
    }

    private UserCouponDTO newReceiveEvent(Long userId, Long couponId, Integer serialNum) {
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }
        UserCouponDTO event = new UserCouponDTO();
        event.setRequestId(UUID.randomUUID().toString());
        event.setUserId(userId);
        event.setCouponId(couponId);
        event.setSerialNum(serialNum);
        return event;
    }

    /**
     * 查询优惠券规则描述（day12 笔记 3.4.2）
     */
    @Override
    public List<String> queryDiscountRules(List<Long> userCouponIds) {
        // 1.查询优惠券信息
        List<Coupon> coupons = baseMapper.queryCouponByUserCouponIds(userCouponIds, UserCouponStatus.USED, UserContext.getUser());
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.转换规则
        return coupons.stream()
                .map(c -> DiscountStrategy.getDiscount(c.getDiscountType()).getRule(c))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询我的优惠券（day10 练习 4.1）
     */
    @Override
    public PageDTO<UserCouponVO> queryMyCoupons(UserCouponQuery query) {
        // 1.分页查询我的用户券
        Integer status = query.getStatus();
        Long userId = UserContext.getUser();
        Page<UserCoupon> page = lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<UserCoupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 2.查询优惠券信息
        List<Long> couponIds = records.stream().map(UserCoupon::getCouponId).collect(Collectors.toList());
        List<Coupon> coupons = couponMapper.selectBatchIds(couponIds);
        Map<Long, Coupon> couponMap = coupons.stream()
                .collect(Collectors.toMap(Coupon::getId, c -> c));
        // 3.组装VO
        List<UserCouponVO> list = new ArrayList<>(records.size());
        for (UserCoupon uc : records) {
            // 3.1.用户券信息（id、status、termEndTime）
            UserCouponVO vo = BeanUtils.copyBean(uc, UserCouponVO.class);
            // 3.2.优惠券信息
            Coupon coupon = couponMap.get(uc.getCouponId());
            if (coupon != null) {
                vo.setName(coupon.getName());
                vo.setSpecific(coupon.getSpecific());
                vo.setDiscountType(coupon.getDiscountType());
                vo.setThresholdAmount(coupon.getThresholdAmount());
                vo.setDiscountValue(coupon.getDiscountValue());
                vo.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
                vo.setTermDays(coupon.getTermDays());
            }
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }

    /**
     * 保存用户券
     */
    private void saveUserCoupon(Coupon coupon, Long userId) {
        // 1.基本信息
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(coupon.getId());
        // 2.有效期信息
        LocalDateTime termBeginTime = coupon.getTermBeginTime();
        LocalDateTime termEndTime = coupon.getTermEndTime();
        if (termBeginTime == null) {
            termBeginTime = LocalDateTime.now();
            termEndTime = termBeginTime.plusDays(coupon.getTermDays());
        }
        uc.setTermBeginTime(termBeginTime);
        uc.setTermEndTime(termEndTime);
        // 3.保存
        if (!save(uc)) {
            throw new DbException("保存用户券失败");
        }
    }
}
