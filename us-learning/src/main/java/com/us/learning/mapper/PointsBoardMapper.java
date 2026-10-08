package com.us.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.us.learning.domain.po.PointsBoard;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 学霸天梯榜 Mapper 接口
 * </p>
 */
public interface PointsBoardMapper extends BaseMapper<PointsBoard> {

    /**
     * 动态创建赛季榜单表（day08 笔记 2.3.3）
     */
    void createPointsBoardTable(@Param("tableName") String tableName);
}