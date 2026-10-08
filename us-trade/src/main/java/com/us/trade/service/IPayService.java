package com.us.trade.service;

import com.us.trade.domain.dto.OrderDelayQueryDTO;
import com.us.trade.domain.dto.PayApplyFormDTO;
import com.us.trade.domain.vo.PayChannelVO;

import java.util.List;

public interface IPayService {
    List<PayChannelVO> queryPayChannels();

    String applyPayOrder(PayApplyFormDTO payApply);

    void queryPayResult(OrderDelayQueryDTO message);
}
