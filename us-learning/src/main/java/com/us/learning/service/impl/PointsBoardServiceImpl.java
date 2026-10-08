package com.us.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.us.api.client.user.UserClient;
import com.us.api.dto.user.UserDTO;
import com.us.common.utils.CollUtils;
import com.us.common.utils.DateUtils;
import com.us.common.utils.UserContext;
import com.us.learning.constants.RedisConstants;
import com.us.learning.domain.po.PointsBoard;
import com.us.learning.domain.query.PointsBoardQuery;
import com.us.learning.domain.vo.PointsBoardItemVO;
import com.us.learning.domain.vo.PointsBoardVO;
import com.us.learning.mapper.PointsBoardMapper;
import com.us.learning.service.IPointsBoardService;
import com.us.learning.utils.TableInfoContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.us.learning.constants.LearningConstants.POINTS_BOARD_TABLE_PREFIX;

/**
 * <p>
 * 学霸天梯榜 服务实现类（day08）
 * 当前赛季榜单：Redis ZSet；历史赛季榜单：按月分表（points_board_{seasonId}）
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {

    private final StringRedisTemplate redisTemplate;

    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        // 1.判断是否是查询当前赛季
        Long season = query.getSeason();
        boolean isCurrent = season == null || season == 0;
        // 2.获取Redis的Key
        LocalDateTime now = LocalDateTime.now();
        String key = RedisConstants.POINTS_BOARD_KEY_PREFIX + now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        // 3.查询我的积分和排名
        PointsBoard myBoard = isCurrent ?
                queryMyCurrentBoard(key) : // 查询当前榜单（Redis）
                queryMyHistoryBoard(season); // 查询历史榜单（MySQL）
        // 4.查询榜单列表
        List<PointsBoard> list = isCurrent ?
                queryCurrentBoardList(key, query.getPageNo(), query.getPageSize()) :
                queryHistoryBoardList(query);
        // 5.封装VO
        PointsBoardVO vo = new PointsBoardVO();
        // 5.1.处理我的信息
        if (myBoard != null) {
            vo.setPoints(myBoard.getPoints());
            vo.setRank(myBoard.getRank());
        }
        if (CollUtils.isEmpty(list)) {
            return vo;
        }
        // 5.2.查询用户信息
        Set<Long> uIds = list.stream()
                .map(PointsBoard::getUserId)
                .collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(uIds);
        Map<Long, String> userMap = new HashMap<>(uIds.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        // 5.3.转换VO
        List<PointsBoardItemVO> items = new ArrayList<>(list.size());
        for (PointsBoard p : list) {
            PointsBoardItemVO v = new PointsBoardItemVO();
            v.setPoints(p.getPoints());
            v.setRank(p.getRank());
            v.setName(userMap.get(p.getUserId()));
            items.add(v);
        }
        vo.setBoardList(items);
        return vo;
    }

    /**
     * 分页查询历史赛季榜单列表（day08 练习3.1，由学员实现）
     */
    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        // 1.设置动态表名（必须在查询前设置，MP插件在解析SQL时读它）
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + query.getSeason());
        try {
            // 2.分页查询：默认按 id 升序（id即排名，第一名在前）
            List<PointsBoard> records = lambdaQuery()
                    .page(query.toMpPage("id", true))
                    .getRecords();
            if (CollUtils.isEmpty(records)) {
                return CollUtils.emptyList();
            }
            // 3.id即排名，写回rank字段供上层转VO
            records.forEach(p -> p.setRank(p.getId()
                    .intValue()));
            return records;
        } finally {
            // 4.解除绑定（必须放在finally，防止查询异常时ThreadLocal残留）
            TableInfoContext.remove();
        }
    }

    @Override
    public List<PointsBoard> queryCurrentBoardList(String key, Integer pageNo, Integer pageSize) {
        // 1.计算分页
        int from = (pageNo - 1) * pageSize;
        // 2.查询（ZSet按score倒序，即积分从高到低）
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, from, from + pageSize - 1);
        if (CollUtils.isEmpty(tuples)) {
            return CollUtils.emptyList();
        }
        // 3.封装
        int rank = from + 1;
        List<PointsBoard> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String userId = tuple.getValue();
            Double points = tuple.getScore();
            if (userId == null || points == null) {
                continue;
            }
            PointsBoard p = new PointsBoard();
            p.setUserId(Long.valueOf(userId));
            p.setPoints(points.intValue());
            p.setRank(rank++);
            list.add(p);
        }
        return list;
    }

    /**
     * 查询我的历史赛季榜单信息（day08 练习3.1，由学员实现）
     */
    private PointsBoard queryMyHistoryBoard(Long season) {
        // 1.设置动态表名
        TableInfoContext.setInfo(POINTS_BOARD_TABLE_PREFIX + season);
        try {
            // 2.获取当前登录用户
            Long userId = UserContext.getUser();
            // 3.查询用户在该赛季榜单里的记录（一个赛季一个用户只有一条）
            PointsBoard board = lambdaQuery()
                    .eq(PointsBoard::getUserId, userId)
                    .one();
            if (board == null) {
                // 没上榜（该赛季没得分），返回null，上层不填"我的排名"
                return null;
            }
            // 4.id即排名
            board.setRank(board.getId()
                    .intValue());
            return board;
        } finally {
            TableInfoContext.remove();
        }
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        // 1.绑定key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        // 2.获取当前用户信息
        String userId = UserContext.getUser()
                .toString();
        // 3.查询积分
        Double points = ops.score(userId);
        // 4.查询排名（reverseRank从0开始，+1才是名次）
        Long rank = ops.reverseRank(userId);
        // 5.封装返回
        PointsBoard p = new PointsBoard();
        p.setPoints(points == null ? 0 : points.intValue());
        p.setRank(rank == null ? 0 : rank.intValue() + 1);
        return p;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable(POINTS_BOARD_TABLE_PREFIX + season);
    }
}