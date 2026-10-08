package com.us.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.common.domain.dto.PageDTO;
import com.us.pay.sdk.dto.RefundResultDTO;
import com.us.trade.domain.dto.ApproveFormDTO;
import com.us.trade.domain.dto.RefundCancelDTO;
import com.us.trade.domain.dto.RefundFormDTO;
import com.us.trade.domain.po.RefundApply;
import com.us.trade.domain.query.RefundApplyPageQuery;
import com.us.trade.domain.vo.RefundApplyPageVO;
import com.us.trade.domain.vo.RefundApplyVO;

import java.util.List;

/**
 * <p>
 * 退款申请 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2022-08-29
 */
public interface IRefundApplyService extends IService<RefundApply> {

    List<RefundApply> queryByDetailId(Long id);

    void applyRefund(RefundFormDTO refundFormDTO);

    PageDTO<RefundApplyPageVO> queryRefundApplyByPage(RefundApplyPageQuery pageQuery);

    RefundApplyVO queryRefundDetailById(Long id);

    RefundApplyVO nextRefundApplyToApprove();

    void approveRefundApply(ApproveFormDTO approveDTO);

    void cancelRefundApply(RefundCancelDTO cancelDTO);

    RefundApplyVO queryRefundDetailByDetailId(Long id);

    void handleRefundResult(RefundResultDTO refundResult);

    List<RefundApply> queryApplyToSend(int page, int size);

    void sendRefundRequest(RefundApply refundApply);

    boolean checkRefundStatus(RefundApply refundApply);
}
