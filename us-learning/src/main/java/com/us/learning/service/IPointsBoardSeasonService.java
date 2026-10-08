package com.us.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.us.learning.domain.po.PointsBoardSeason;
import com.us.learning.domain.vo.PointsBoardSeasonVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 学霸天梯榜赛季 服务类（day07 练习4.3）
 * </p>
 */
public interface IPointsBoardSeasonService extends IService<PointsBoardSeason> {

    /**
     * 查询全部历史赛季列表
     * @return 赛季列表
     */
    List<PointsBoardSeasonVO> listSeasons();

    /**
     * 根据时间查询所属赛季id（day08 笔记 2.3.2）
     * @param time 时间
     * @return 赛季id，找不到返回null
     */
    Integer querySeasonByTime(LocalDateTime time);
}