package com.us.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.learning.domain.po.PointsBoard;
import com.us.learning.domain.query.PointsBoardQuery;
import com.us.learning.domain.vo.PointsBoardVO;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 */
public interface IPointsBoardService extends IService<PointsBoard> {

    /**
     * 查询指定赛季的积分排行榜以及当前用户的积分和排名（day08 笔记 1.3）
     * @param query 分页参数 + 赛季id（空或0代表当前赛季）
     * @return 我的排名积分 + 榜单列表
     */
    PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query);

    /**
     * 创建指定赛季的榜单表（day08 笔记 2.3.3）
     * @param season 赛季id
     */
    void createPointsBoardTableBySeason(Integer season);

    /**
     * 分页查询当前赛季榜单列表（从Redis ZSet读取，day08 笔记 2.4.3）
     * @param key Redis榜单KEY
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 榜单列表（含名次）
     */
    List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize);
}