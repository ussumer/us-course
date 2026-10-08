package com.us.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.api.client.remark.RemarkClient;
import com.us.api.client.user.UserClient;
import com.us.api.dto.user.UserDTO;
import com.us.common.autoconfigure.mq.RabbitMqHelper;
import com.us.common.constants.MqConstants;
import com.us.common.domain.dto.PageDTO;
import com.us.common.exceptions.BadRequestException;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.common.utils.UserContext;
import com.us.learning.domain.dto.ReplyDTO;
import com.us.learning.domain.po.InteractionQuestion;
import com.us.learning.domain.po.InteractionReply;
import com.us.learning.domain.query.ReplyPageQuery;
import com.us.learning.domain.vo.ReplyVO;
import com.us.learning.enums.QuestionStatus;
import com.us.learning.mapper.InteractionReplyMapper;
import com.us.learning.service.IInteractionQuestionService;
import com.us.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的回答或评论 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements
        IInteractionReplyService {

    private final UserClient userClient;
    private final IInteractionQuestionService questionService;
    private final RemarkClient remarkClient;
    private final RabbitMqHelper mqHelper;
    /**
     * 新增回答或评论（day05 练习4.1）——业务逻辑由学员实现
     */
    @Override
    public void saveReply(ReplyDTO replyDTO) {
        Long userId = UserContext.getUser();
        InteractionReply reply = BeanUtils.toBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        if (replyDTO.getAnswerId() == null) {
            reply.setAnswerId(0L);
        }
        save(reply);
        if (replyDTO.getAnswerId() == null) {
            questionService.lambdaUpdate()
                    .set(InteractionQuestion::getLatestAnswerId, reply.getId())
                    .setSql("answer_times =answer_times +1")
                    .eq(InteractionQuestion::getId, reply.getQuestionId())
                    .update();
        }
        if (Boolean.TRUE.equals(replyDTO.getIsStudent())) {
            questionService.lambdaUpdate()
                    .set(InteractionQuestion::getStatus, QuestionStatus.UN_CHECK)
                     .eq(InteractionQuestion::getId, replyDTO.getQuestionId())
                    .update();
        }
        // 7.发送MQ消息，通知积分系统（day07 练习4.2：写回答得5分，评论不计分）
        if (replyDTO.getAnswerId() == null) {
            mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.WRITE_REPLY, userId);
        }
    }

    /**
     * 用户端分页查询回答或评论
     */
    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        return queryReplyPage(query, false);
    }

    /**
     * 管理端分页查询回答或评论
     */
    @Override
    public PageDTO<ReplyVO> queryReplyPageAdmin(ReplyPageQuery query) {
        return queryReplyPage(query, true);
    }

    /**
     * 分页查询回答/评论的公共实现（day05 练习4.2 + 4.3 抽取，避免重复代码）
     *
     * @param admin true-管理端（统计包含隐藏、无视匿名）；false-用户端（不展示隐藏、匿名不给用户信息）
     */
    private PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query, boolean admin) {
        // 1.校验：问题id和回答id不能都为空
        Long questionId = query.getQuestionId();
        Long answerId = query.getAnswerId();
        if (questionId == null && answerId == null) {
            throw new BadRequestException("问题id和回答id不能都为空");
        }
        // 2.分页查询
        //    answerId有值 = 查该回答下的评论；answerId无值 = 查该问题下的回答（answer_id = 0）
        //    默认按点赞次数倒序（前端可通过sortBy覆盖）
        Page<InteractionReply> page = lambdaQuery()
                .eq(questionId != null, InteractionReply::getQuestionId, questionId)
                .eq(answerId != null, InteractionReply::getAnswerId, answerId)
                .eq(!admin, InteractionReply::getHidden, false) // 用户端不展示被隐藏的
                .page(query.toMpPage("liked_times", false));
        List<InteractionReply> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.收集用户id：回复者 + 目标用户（评论特有），管理端无视匿名
        Set<Long> userIds = new HashSet<>();
        for (InteractionReply r : records) {
            if (admin || !r.getAnonymity()) {
                userIds.add(r.getUserId());
            }
            if (r.getTargetUserId() != null && r.getTargetUserId() > 0) {
                userIds.add(r.getTargetUserId());
            }
        }
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            if (CollUtils.isNotEmpty(users)) {
                userMap = users.stream()
                        .collect(Collectors.toMap(UserDTO::getId, u -> u));
            }
        }
        // 4.一次性批量查询当前用户点过赞的业务id
        // 注意：Feign远程调用必须在循环【外面】，循环里只做O(1)的contains判断
        Set<Long> bizIds = records.stream().map(InteractionReply::getId).collect(Collectors.toSet());
        Set<Long> likedIds = remarkClient.isBizLiked(bizIds);
        // 5.封装VO
        List<ReplyVO> voList = new ArrayList<>(records.size());
        for (InteractionReply r : records) {
            // 4.1.PO转VO（id、content、anonymity、hidden、replyTimes、createTime、likedTimes）
            ReplyVO vo = BeanUtils.copyBean(r, ReplyVO.class);
            voList.add(vo);
            // 4.2.回复者信息（用户端匿名不返回用户信息）
            if (admin || !r.getAnonymity()) {
                UserDTO user = userMap.get(r.getUserId());
                if (user != null) {
                    vo.setUserId(user.getId());
                    vo.setUserName(user.getName());
                    vo.setUserIcon(user.getIcon());
                    vo.setUserType(user.getType());
                }
            } else {
                vo.setUserId(null);
            }
            // 4.3.目标用户昵称（评论特有，显示为"张三评论了李四"）
            if (r.getTargetUserId() != null && r.getTargetUserId() > 0) {
                UserDTO target = userMap.get(r.getTargetUserId());
                if (target != null) {
                    vo.setTargetUserName(target.getName());
                }
            }
            // 4.4.是否点过赞（likedIds来自循环外的批量查询，这里只做判断）
            vo.setLiked(likedIds.contains(r.getId()));
        }
        return PageDTO.of(page, voList);
    }

    /**
     * 管理端隐藏或显示回答或评论（day05 练习4.4）
     */
    @Override
    public void hiddenReply(Long id, Boolean hidden) {
        // 1.查询当前回复
        InteractionReply reply = getById(id);
        if (reply == null) {
            throw new BadRequestException("回答或评论不存在");
        }
        // 2.更新自身的hidden状态
        lambdaUpdate()
                .set(InteractionReply::getHidden, hidden)
                .eq(InteractionReply::getId, id)
                .update();
        // 3.如果隐藏/显示的是回答（answer_id=0），连带处理其下所有评论
        if (Long.valueOf(0L)
                .equals(reply.getAnswerId())) {
            lambdaUpdate()
                    .set(InteractionReply::getHidden, hidden)
                    .eq(InteractionReply::getAnswerId, id)
                    .update();
        }
    }
}
