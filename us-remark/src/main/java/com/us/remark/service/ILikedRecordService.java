package com.us.remark.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.remark.domain.dto.LikeRecordFormDTO;
import com.us.remark.domain.po.LikedRecord;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 */
public interface ILikedRecordService extends IService<LikedRecord> {

    /**
     * 点赞或取消点赞
     * @param recordFormDTO 点赞表单
     */
    void addLikeRecord(LikeRecordFormDTO recordFormDTO);

    /**
     * 批量查询当前用户对指定业务的点赞状态
     * @param bizIds 业务id集合
     * @return 当前用户已点赞的业务id集合
     */
    Set<Long> isBizLiked(List<Long> bizIds);

    /**
     * 读取Redis中缓存的点赞数变更并通过MQ发送给业务方（定时任务调用）
     * @param bizType 业务类型
     * @param maxBizSize 单次最多处理的业务数量
     */
    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);
}