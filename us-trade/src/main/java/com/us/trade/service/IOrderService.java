package com.us.trade.service;

import com.us.common.domain.dto.PageDTO;
import com.us.pay.sdk.dto.PayResultDTO;
import com.us.trade.domain.dto.PlaceOrderDTO;
import com.us.trade.domain.po.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.us.trade.domain.po.OrderDetail;
import com.us.trade.domain.query.OrderPageQuery;
import com.us.trade.domain.vo.OrderConfirmVO;
import com.us.trade.domain.vo.OrderPageVO;
import com.us.trade.domain.vo.OrderVO;
import com.us.trade.domain.vo.PlaceOrderResultVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 订单 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-08-29
 */
public interface IOrderService extends IService<Order> {

    PlaceOrderResultVO placeOrder(PlaceOrderDTO placeOrderDTO);

    @Transactional
    void saveOrderAndDetails(Order order, List<OrderDetail> orderDetails);

    void cancelOrder(Long orderId);

    void deleteOrder(Long id);

    PageDTO<OrderPageVO> queryMyOrderPage(OrderPageQuery pageQuery);

    OrderVO queryOrderById(Long id);

    PlaceOrderResultVO queryOrderStatus(Long orderId);

    void handlePaySuccess(PayResultDTO payResult);

    PlaceOrderResultVO enrolledFreeCourse(Long courseId);

    OrderConfirmVO prePlaceOrder(List<Long> courseIds);

}
