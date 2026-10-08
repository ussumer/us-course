package com.us.learning.controller;

import com.us.learning.domain.query.PointsBoardQuery;
import com.us.learning.domain.vo.PointsBoardSeasonVO;
import com.us.learning.domain.vo.PointsBoardVO;
import com.us.learning.service.IPointsBoardSeasonService;
import com.us.learning.service.IPointsBoardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜相关接口
 * </p>
 */
@RestController
@RequestMapping("/boards")
@Api(tags = "学霸天梯榜相关接口")
@RequiredArgsConstructor
public class PointsBoardController {

    private final IPointsBoardSeasonService seasonService;

    private final IPointsBoardService pointsBoardService;

    /**
     * 查询历史赛季列表（day07 练习4.3）
     */
    @ApiOperation("查询历史赛季列表")
    @GetMapping("/seasons/list")
    public List<PointsBoardSeasonVO> querySeasons(){
        return seasonService.listSeasons();
    }

    /**
     * 分页查询指定赛季的积分排行榜以及当前用户的积分和排名（day08 笔记 1.3.3）
     * season为空或0代表查询当前赛季
     */
    @ApiOperation("分页查询指定赛季的积分排行榜")
    @GetMapping
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query){
        return pointsBoardService.queryPointsBoardBySeason(query);
    }
}