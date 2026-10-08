package com.us.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.common.utils.BeanUtils;
import com.us.common.utils.CollUtils;
import com.us.learning.domain.po.PointsBoardSeason;
import com.us.learning.domain.vo.PointsBoardSeasonVO;
import com.us.learning.mapper.PointsBoardSeasonMapper;
import com.us.learning.service.IPointsBoardSeasonService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 学霸天梯榜赛季 服务实现类（day07 练习4.3）
 * </p>
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    @Override
    @SuppressWarnings("unchecked") // MP的orderByDesc(R... columns)为varargs设计，单列调用必然创建泛型数组，MP内部实现安全
    public List<PointsBoardSeasonVO> listSeasons() {
        // 1.查询全部赛季（按赛季id倒序，最新的在前面）
        List<PointsBoardSeason> list = lambdaQuery()
                .orderByDesc(PointsBoardSeason::getId)
                .list();
        if (CollUtils.isEmpty(list)) {
            return CollUtils.emptyList();
        }
        // 2.转VO返回
        return BeanUtils.copyList(list, PointsBoardSeasonVO.class);
    }

    /**
     * 根据时间查询所属赛季id（day08 笔记 2.3.2）：begin_time <= time <= end_time
     */
    @Override
    public Integer querySeasonByTime(LocalDateTime time) {
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .oneOpt();
        return optional.map(PointsBoardSeason::getId).orElse(null);
    }
}