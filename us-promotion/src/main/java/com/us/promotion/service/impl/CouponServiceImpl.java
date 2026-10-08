package com.us.promotion.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.api.cache.CategoryCache;
import com.us.api.dto.course.CategoryBasicDTO;
import com.us.common.domain.dto.PageDTO;
import com.us.common.exceptions.BadRequestException;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.utils.AssertUtils;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.NumberUtils;
import com.us.common.utils.StringUtils;
import com.us.common.utils.UserContext;
import com.us.promotion.domain.dto.CouponFormDTO;
import com.us.promotion.domain.dto.CouponIssueFormDTO;
import com.us.promotion.domain.po.Coupon;
import com.us.promotion.domain.po.CouponScope;
import com.us.promotion.domain.po.UserCoupon;
import com.us.promotion.domain.query.CouponQuery;
import com.us.promotion.domain.vo.CouponDetailVO;
import com.us.promotion.domain.vo.CouponPageVO;
import com.us.promotion.domain.vo.CouponScopeVO;
import com.us.promotion.domain.vo.CouponVO;
import com.us.promotion.enums.CouponStatus;
import com.us.promotion.enums.ObtainType;
import com.us.promotion.enums.UserCouponStatus;
import com.us.promotion.mapper.CouponMapper;
import com.us.promotion.service.ICouponScopeService;
import com.us.promotion.service.ICouponService;
import com.us.promotion.service.IExchangeCodeService;
import com.us.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.us.promotion.constants.PromotionConstants.COUPON_CACHE_KEY_PREFIX;
import static com.us.promotion.constants.PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    private final ICouponScopeService scopeService;

    private final IExchangeCodeService codeService;

    private final IUserCouponService userCouponService;

    private final CategoryCache categoryCache;

    private final StringRedisTemplate redisTemplate;

    /**
     * 新增优惠券（笔记 2.1.3）
     */
    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // 1.保存优惠券
        // 1.1.转PO
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        // 1.2.保存
        save(coupon);

        if (!Boolean.TRUE.equals(dto.getSpecific())) {
            // 没有范围限定
            return;
        }
        // 2.保存限定范围
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BadRequestException("限定范围不能为空");
        }
        saveScope(scopes, coupon.getId());
    }

    /**
     * 分页查询优惠券（笔记 2.2.3）
     */
    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Integer status = query.getStatus();
        String name = query.getName();
        Integer type = query.getType();
        // 1.分页查询
        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        // 2.处理VO
        List<Coupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
        // 3.返回
        return PageDTO.of(page, list);
    }

    /**
     * 查询发放中的优惠券（day10 笔记 2.1.3）
     */
    @Override
    public List<CouponVO> queryIssuingCoupons() {
        // 1.查询发放中的优惠券列表
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.统计当前用户已经领取的优惠券的信息
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        // 2.1.查询当前用户已经领取的优惠券的数据
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        // 2.2.统计当前用户对优惠券的已经领取数量
        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.3.统计当前用户对优惠券的已经领取并且未使用的数量
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 3.封装VO结果
        List<CouponVO> list = new ArrayList<>(coupons.size());
        for (Coupon c : coupons) {
            // 3.1.拷贝PO属性到VO
            CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);
            list.add(vo);
            // 3.2.是否可以领取：已经被领取的数量 < 优惠券总数量 && 当前用户已经领取的数量 < 每人限领数量
            Object cachedStock = redisTemplate.opsForHash().get(COUPON_CACHE_KEY_PREFIX + c.getId(), "totalNum");
            Object cachedCount = UserContext.getUser() == null ? null : redisTemplate.opsForHash().get(
                    USER_COUPON_CACHE_KEY_PREFIX + c.getId(), UserContext.getUser().toString());
            int stock = cachedStock == null ? 0 : Integer.parseInt(cachedStock.toString());
            long receivedCount = cachedCount == null ? issuedMap.getOrDefault(c.getId(), 0L) : Long.parseLong(cachedCount.toString());
            vo.setRemainingNum(Math.max(0, stock));
            vo.setUserLimit(c.getUserLimit());
            LocalDateTime now = LocalDateTime.now();
            vo.setAvailable(stock > 0 && receivedCount < c.getUserLimit()
                    && c.getIssueBeginTime() != null && !now.isBefore(c.getIssueBeginTime())
                    && c.getIssueEndTime() != null && !now.isAfter(c.getIssueEndTime()));
            // 3.3.是否可以使用：当前用户已经领取并且未使用的优惠券数量 > 0
            vo.setReceived(unusedMap.getOrDefault(c.getId(), 0L) > 0);
        }
        return list;
    }

    /**
     * 发放优惠券（笔记 3.1.3 + day11 2.2.3：立刻发放时预热Redis缓存）
     */
    @Transactional
    @Override
    public void beginIssue(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2.判断优惠券状态，是否是暂停或待发放
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            throw new BizIllegalException("优惠券状态错误！");
        }
        // 3.判断是否是立刻发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin = issueBeginTime == null || !issueBeginTime.isAfter(now);
        // 4.更新优惠券
        // 4.1.拷贝属性到PO
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        // 4.2.更新状态
        if (isBegin) {
            c.setStatus(CouponStatus.ISSUING);
            c.setIssueBeginTime(now);
        } else {
            c.setStatus(CouponStatus.UN_ISSUE);
        }
        // 4.3.写入数据库
        updateById(c);

        // 5.添加缓存，前提是立刻发放的优惠券（day11 2.2.3：领券资格校验基于缓存完成）
        if (isBegin) {
            coupon.setIssueBeginTime(c.getIssueBeginTime());
            coupon.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupon);
        }

        // 6.判断是否需要生成兑换码：领取方式必须是兑换码，且之前状态必须是待发放（暂停后重新发放不重复生成）
        if (coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT) {
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupon);
        }
    }

    /**
     * 修改优惠券（笔记 4.1 练习：只有待发放状态可以修改）
     */
    @Override
    @Transactional
    public void updateCoupon(CouponFormDTO dto) {
        // 1.校验id
        Long couponId = dto.getId();
        AssertUtils.isNotNull(couponId, "优惠券id不能为空");
        // 2.查询优惠券
        Coupon coupon = getById(couponId);
        AssertUtils.isNotNull(coupon, "优惠券不存在！");
        // 3.只有待发放状态可以修改
        if (coupon.getStatus() != CouponStatus.DRAFT) {
            throw new BizIllegalException("优惠券状态错误，只有待发放的优惠券可以修改！");
        }
        // 4.更新优惠券本身（hutool拷贝忽略null，dto中没有的字段不受影响）
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        updateById(c);
        // 5.更新使用范围：先删除旧的范围，再保存新的
        removeScope(couponId);
        if (Boolean.TRUE.equals(dto.getSpecific())) {
            List<Long> scopes = dto.getScopes();
            if (CollUtils.isEmpty(scopes)) {
                throw new BadRequestException("限定范围不能为空");
            }
            saveScope(scopes, couponId);
        }
    }

    /**
     * 删除优惠券（笔记 4.2 练习：只有待发放状态可以删除）
     */
    @Override
    @Transactional
    public void deleteCouponById(Long id) {
        // 1.查询优惠券
        Coupon coupon = getById(id);
        AssertUtils.isNotNull(coupon, "优惠券不存在！");
        // 2.只有待发放状态可以删除
        if (coupon.getStatus() != CouponStatus.DRAFT) {
            throw new BizIllegalException("优惠券状态错误，只有待发放的优惠券可以删除！");
        }
        // 3.删除优惠券
        removeById(id);
        // 4.删除对应的使用范围
        removeScope(id);
    }

    /**
     * 根据id查询优惠券详情（笔记 4.3 练习）
     */
    @Override
    public CouponDetailVO queryCouponById(Long id) {
        // 1.查询优惠券
        Coupon coupon = getById(id);
        AssertUtils.isNotNull(coupon, "优惠券不存在！");
        // 2.转VO
        CouponDetailVO vo = BeanUtils.copyBean(coupon, CouponDetailVO.class);
        // 3.判断是否有范围限定
        if (!Boolean.TRUE.equals(coupon.getSpecific())) {
            return vo;
        }
        // 3.1.查询范围
        List<CouponScope> scopes = scopeService.lambdaQuery()
                .eq(CouponScope::getCouponId, id)
                .list();
        if (CollUtils.isEmpty(scopes)) {
            return vo;
        }
        // 3.2.查询分类信息（走本地Caffeine缓存，不直接调Feign）
        List<Long> cIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toList());
        List<CategoryBasicDTO> categories = categoryCache.queryCategoryByIds(cIds);
        List<CouponScopeVO> scopeVOS = new ArrayList<>(categories.size());
        for (CategoryBasicDTO category : categories) {
            if (category == null) {
                // 分类可能已被删除，跳过
                continue;
            }
            scopeVOS.add(new CouponScopeVO(category.getId(), category.getName()));
        }
        vo.setScopes(scopeVOS);
        return vo;
    }

    /**
     * 暂停发放优惠券（笔记 4.6 练习 + day11 2.2.4：同时清理缓存）
     */
    @Override
    @Transactional
    public void pauseCoupon(Long id) {
        // 1.查询优惠券
        Coupon coupon = getById(id);
        AssertUtils.isNotNull(coupon, "优惠券不存在！");
        // 2.只有未开始或发放中状态可以暂停
        CouponStatus status = coupon.getStatus();
        if (status != CouponStatus.UN_ISSUE && status != CouponStatus.ISSUING) {
            throw new BizIllegalException("优惠券状态错误，只有未开始或发放中的优惠券可以暂停！");
        }
        // 3.更新状态（带状态条件的更新，避免并发重复操作）
        boolean success = lambdaUpdate()
                .set(Coupon::getStatus, CouponStatus.PAUSE)
                .eq(Coupon::getId, id)
                .in(Coupon::getStatus, CouponStatus.UN_ISSUE, CouponStatus.ISSUING)
                .update();
        if (!success) {
            log.error("重复暂停优惠券，id={}", id);
        }
        // 4.删除缓存，暂停后无法领取
        redisTemplate.delete(COUPON_CACHE_KEY_PREFIX + id);
    }

    /**
     * 定时开始发放优惠券（day09 练习 4.4 + day11 2.2.3：同时预热缓存）
     * 未开始 → 发放中，只处理发放时间已到的券
     */
    @Override
    public void beginIssueCoupon(int shardIndex, int shardTotal) {
        LocalDateTime now = LocalDateTime.now();
        // 1.查询到点应发放的券（数据分片，避免多实例重复处理）
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .le(Coupon::getIssueBeginTime, now)
                .apply("id % {0} = {1}", shardTotal, shardIndex)
                .list();
        if (CollUtils.isEmpty(coupons)) {
            return;
        }
        // 2.批量更新状态：未开始 → 发放中（带状态条件，保证幂等）
        List<Long> ids = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        lambdaUpdate()
                .set(Coupon::getStatus, CouponStatus.ISSUING)
                .in(Coupon::getId, ids)
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .update();
        // 3.预热缓存（day11 2.2.3：定时发放的券也要写缓存，否则用户领取时LUA脚本校验不通过）
        coupons.forEach(this::cacheCouponInfo);
    }

    /**
     * 定时结束发放优惠券（day09 练习 4.5）
     * 发放中 → 发放结束，只处理领用截止时间已过的券
     */
    @Override
    public void finishIssueCoupon(int shardIndex, int shardTotal) {
        LocalDateTime now = LocalDateTime.now();
        lambdaUpdate()
                .set(Coupon::getStatus, CouponStatus.FINISHED)
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .le(Coupon::getIssueEndTime, now)
                .apply("id % {0} = {1}", shardTotal, shardIndex)
                .update();
        // 说明：此处不删除缓存。保留缓存可以让LUA脚本返回更准确的提示（"发放已经结束"而不是"优惠券不存在"）
    }

    /**
     * 保存优惠券限定范围（新增与修改共用）
     */
    private void saveScope(List<Long> scopes, Long couponId) {
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setType(1).setBizId(bizId).setCouponId(couponId))
                .collect(Collectors.toList());
        scopeService.saveBatch(list);
    }

    /**
     * 删除优惠券的限定范围
     */
    private void removeScope(Long couponId) {
        scopeService.remove(Wrappers.<CouponScope>lambdaQuery()
                .eq(CouponScope::getCouponId, couponId));
    }

    /**
     * 缓存优惠券的领取资格校验信息（day11 2.2.3）
     * 注意1：时间存的是【秒级】时间戳，与LUA脚本中 redis.call('time')[1] 的单位保持一致
     * 注意2：totalNum存的是【剩余库存】= 总数量 - 已发放数量。
     *        笔记原版直接存总数量，但"暂停后重新发放"时已发放数量不为0，
     *        会导致缓存库存虚高，用户领券提示成功而DB扣减失败（乐观锁返回0）
     */
    private void cacheCouponInfo(Coupon coupon) {
        // 1.组织数据
        Map<String, String> map = new HashMap<>(4);
        if (coupon.getIssueBeginTime() != null) {
            map.put("issueBeginTime", String.valueOf(coupon.getIssueBeginTime().toEpochSecond(ZoneOffset.ofHours(8))));
        }
        if (coupon.getIssueEndTime() != null) {
            map.put("issueEndTime", String.valueOf(coupon.getIssueEndTime().toEpochSecond(ZoneOffset.ofHours(8))));
        }
        long remainNum = coupon.getTotalNum() - NumberUtils.null2Zero(coupon.getIssueNum());
        map.put("totalNum", String.valueOf(remainNum));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));
        // 2.写缓存
        redisTemplate.opsForHash().putAll(COUPON_CACHE_KEY_PREFIX + coupon.getId(), map);
    }
}
