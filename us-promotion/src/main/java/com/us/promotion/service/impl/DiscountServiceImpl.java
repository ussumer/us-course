package com.us.promotion.service.impl;

import com.us.api.dto.promotion.CouponDiscountDTO;
import com.us.api.dto.promotion.OrderCouponDTO;
import com.us.api.dto.promotion.OrderCourseDTO;
import com.us.common.utils.CollUtils;
import com.us.common.exceptions.BizIllegalException;
import com.us.common.utils.UserContext;
import com.us.promotion.domain.po.Coupon;
import com.us.promotion.domain.po.CouponScope;
import com.us.promotion.enums.UserCouponStatus;
import com.us.promotion.mapper.UserCouponMapper;
import com.us.promotion.service.ICouponScopeService;
import com.us.promotion.service.IDiscountService;
import com.us.promotion.strategy.discount.Discount;
import com.us.promotion.strategy.discount.DiscountStrategy;
import com.us.promotion.utils.PermuteUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠方案计算（day12 笔记 2 / 3.1）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements IDiscountService {

    private final UserCouponMapper userCouponMapper;

    private final ICouponScopeService scopeService;

    private final Executor discountSolutionExecutor;

    @Override
    public List<CouponDiscountDTO> findDiscountSolution(List<OrderCourseDTO> orderCourses) {
        if (CollUtils.isEmpty(orderCourses)) {
            return CollUtils.emptyList();
        }
        // 1.查询我的所有可用优惠券
        List<Coupon> coupons = userCouponMapper.queryMyCoupons(UserContext.getUser());
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.初筛
        // 2.1.计算订单总价
        int totalAmount = orderCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
        // 2.2.筛选可用券
        List<Coupon> availableCoupons = coupons.stream()
                .filter(c -> DiscountStrategy.getDiscount(c.getDiscountType()).canUse(totalAmount, c))
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(availableCoupons)) {
            return CollUtils.emptyList();
        }
        // 3.排列组合出所有方案
        // 3.1.细筛（找出每一个优惠券的可用的课程，判断课程总价是否达到优惠券的使用需求）
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(availableCoupons, orderCourses);
        if (CollUtils.isEmpty(availableCouponMap)) {
            return CollUtils.emptyList();
        }
        // 3.2.排列组合
        availableCoupons = new ArrayList<>(availableCouponMap.keySet());
        if (availableCoupons.size() > 6) {
            throw new BizIllegalException("可用券超过6张，请选择优惠券后查询指定方案，避免组合计算过载");
        }
        List<List<Coupon>> solutions = PermuteUtil.permute(availableCoupons);
        // 3.3.添加单券的方案
        for (Coupon c : availableCoupons) {
            solutions.add(List.of(c));
        }

        List<CompletableFuture<CouponDiscountDTO>> futures = new ArrayList<>();
        try {
            for (List<Coupon> solution : solutions) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> calculateSolutionDiscount(availableCouponMap, orderCourses, solution),
                        discountSolutionExecutor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get(1, TimeUnit.SECONDS);
            return findBestSolution(futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("优惠方案计算被中断，请重试");
        } catch (Exception e) {
            log.warn("优惠方案计算失败或超时", e);
            throw new BizIllegalException("优惠方案计算繁忙，请稍后重试");
        } finally {
            futures.forEach(f -> { if (!f.isDone()) f.cancel(true); });
        }
    }

    @Override
    public CouponDiscountDTO queryDiscountDetailByOrder(OrderCouponDTO orderCouponDTO) {
        // 1.查询用户优惠券
        List<Long> userCouponIds = orderCouponDTO.getUserCouponIds();
        if (CollUtils.isEmpty(userCouponIds) || userCouponIds.size() > 6) {
            throw new BizIllegalException("请选择1至6张优惠券");
        }
        List<Coupon> coupons = userCouponMapper.queryCouponByUserCouponIds(userCouponIds, UserCouponStatus.UNUSED, UserContext.getUser());
        if (CollUtils.isEmpty(coupons) || coupons.size() != new HashSet<>(userCouponIds).size()) {
            throw new BizIllegalException("优惠券不存在、不属于当前用户或已经失效");
        }
        // 2.查询优惠券对应课程
        Map<Coupon, List<OrderCourseDTO>> availableCouponMap = findAvailableCoupon(coupons, orderCouponDTO.getCourseList());
        if (CollUtils.isEmpty(availableCouponMap)) {
            return null;
        }
        // 3.查询优惠券规则
        return calculateSolutionDiscount(availableCouponMap, orderCouponDTO.getCourseList(), coupons);
    }

    /**
     * 细筛：找出每张券的可用课程，并判断课程总价是否满足门槛
     *
     * @return key为优惠券，value为该券可用的课程
     */
    private Map<Coupon, List<OrderCourseDTO>> findAvailableCoupon(
            List<Coupon> coupons, List<OrderCourseDTO> courses) {
        Map<Coupon, List<OrderCourseDTO>> map = new HashMap<>(coupons.size());
        for (Coupon coupon : coupons) {
            // 1.找出优惠券的可用的课程
            List<OrderCourseDTO> availableCourses = courses;
            if (Boolean.TRUE.equals(coupon.getSpecific())) {
                // 1.1.限定了范围，查询券的可用范围
                List<CouponScope> scopes = scopeService.lambdaQuery()
                        .eq(CouponScope::getCouponId, coupon.getId()).list();
                // 1.2.获取范围对应的分类id
                Set<Long> scopeIds = scopes.stream().map(CouponScope::getBizId).collect(Collectors.toSet());
                // 1.3.筛选课程
                availableCourses = courses.stream()
                        .filter(c -> scopeIds.contains(c.getCateId())).collect(Collectors.toList());
            }
            if (CollUtils.isEmpty(availableCourses)) {
                // 没有任何可用课程，抛弃
                continue;
            }
            // 2.计算课程总价
            int totalAmount = availableCourses.stream().mapToInt(OrderCourseDTO::getPrice).sum();
            // 3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (discount.canUse(totalAmount, coupon)) {
                map.put(coupon, availableCourses);
            }
        }
        return map;
    }

    /**
     * 计算一个优惠方案的优惠金额与优惠明细
     */
    private CouponDiscountDTO calculateSolutionDiscount(
            Map<Coupon, List<OrderCourseDTO>> couponMap, List<OrderCourseDTO> courses, List<Coupon> solution) {
        // 1.初始化DTO
        CouponDiscountDTO dto = new CouponDiscountDTO();
        // 2.初始化折扣明细的映射
        Map<Long, Integer> detailMap = courses.stream().collect(Collectors.toMap(OrderCourseDTO::getId, oc -> 0));
        // 3.计算折扣
        for (Coupon coupon : solution) {
            // 3.1.获取优惠券限定范围对应的课程
            List<OrderCourseDTO> availableCourses = couponMap.get(coupon);
            if (CollUtils.isEmpty(availableCourses)) {
                // 该券没有可用课程（细筛中被剔除），跳过
                continue;
            }
            // 3.2.计算课程总价(课程原价 - 折扣明细)
            int totalAmount = availableCourses.stream()
                    .mapToInt(oc -> oc.getPrice() - detailMap.get(oc.getId())).sum();
            // 3.3.判断是否可用
            Discount discount = DiscountStrategy.getDiscount(coupon.getDiscountType());
            if (totalAmount <= 0 || !discount.canUse(totalAmount, coupon)) {
                // 券不可用，跳过
                continue;
            }
            // 3.4.计算优惠金额
            int discountAmount = discount.calculateDiscount(totalAmount, coupon);
            // 3.5.计算优惠明细
            calculateDiscountDetails(detailMap, availableCourses, totalAmount, discountAmount);
            // 3.6.更新DTO数据
            dto.getIds().add(coupon.getCreater());
            dto.getRules().add(discount.getRule(coupon));
            dto.setDiscountAmount(discountAmount + dto.getDiscountAmount());
        }
        // 4.记录优惠明细（day12 笔记 3.1.3：计算订单优惠明细时需要返回）
        dto.setDiscountDetail(detailMap);
        return dto;
    }

    /**
     * 计算优惠明细：按课程价格占比分摊优惠金额，最后一门课程用减法兜底避免精度损失
     */
    private void calculateDiscountDetails(Map<Long, Integer> detailMap, List<OrderCourseDTO> courses,
                                          int totalAmount, int discountAmount) {
        int times = 0;
        int remainDiscount = discountAmount;
        for (OrderCourseDTO course : courses) {
            // 更新课程已计算数量
            times++;
            int discount;
            // 判断是否是最后一个课程
            if (times == courses.size()) {
                // 是最后一个课程，总折扣金额 - 之前所有商品的折扣金额之和
                discount = remainDiscount;
            } else {
                // 计算折扣明细（课程价格在总价中占的比例，乘以总的折扣）
                discount = (int) ((long) discountAmount * (course.getPrice() - detailMap.get(course.getId())) / totalAmount);
                remainDiscount -= discount;
            }
            // 更新折扣明细
            detailMap.put(course.getId(), discount + detailMap.get(course.getId()));
        }
    }

    /**
     * 筛选最优解：用券相同时取优惠最高，优惠相同时取用券最少
     */
    private List<CouponDiscountDTO> findBestSolution(List<CouponDiscountDTO> list) {
        // 1.准备Map记录最优解
        Map<String, CouponDiscountDTO> moreDiscountMap = new HashMap<>();
        Map<Integer, CouponDiscountDTO> lessCouponMap = new HashMap<>();
        // 2.遍历，筛选最优解
        for (CouponDiscountDTO solution : list) {
            // 2.1.计算当前方案的id组合
            String ids = solution.getIds().stream()
                    .sorted(Long::compare).map(String::valueOf).collect(Collectors.joining(","));
            // 2.2.比较用券相同时，优惠金额是否最大
            CouponDiscountDTO best = moreDiscountMap.get(ids);
            if (best != null && best.getDiscountAmount() >= solution.getDiscountAmount()) {
                // 当前方案优惠金额少，跳过
                continue;
            }
            // 2.3.比较金额相同时，用券数量是否最少
            best = lessCouponMap.get(solution.getDiscountAmount());
            int size = solution.getIds().size();
            if (size > 1 && best != null && best.getIds().size() <= size) {
                // 当前方案用券更多，放弃
                continue;
            }
            // 2.4.更新最优解
            moreDiscountMap.put(ids, solution);
            lessCouponMap.put(solution.getDiscountAmount(), solution);
        }
        // 3.求交集（笔记原文用 CollUtils.intersection，本项目 CollUtils 未提供该方法，改用标准集合求交）
        Set<CouponDiscountDTO> bestSolutions = new HashSet<>(moreDiscountMap.values());
        bestSolutions.retainAll(new HashSet<>(lessCouponMap.values()));
        // 4.排序，按优惠金额降序
        return bestSolutions.stream()
                .sorted(Comparator.comparingInt(CouponDiscountDTO::getDiscountAmount).reversed())
                .collect(Collectors.toList());
    }
}
