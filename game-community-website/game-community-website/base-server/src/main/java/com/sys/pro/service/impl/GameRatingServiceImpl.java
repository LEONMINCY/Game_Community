package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.GameRating;
import com.sys.pro.pojo.Order;
import com.sys.pro.service.GameRatingService;
import com.sys.pro.mapper.GameRatingMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.service.OrderService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.sys.pro.mapper.UserInfoMapper;
import com.sys.pro.pojo.UserInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 游戏评价服务实现，统计评分、好评率和玩家评价分页数据。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class GameRatingServiceImpl extends ServiceImpl<GameRatingMapper, GameRating> implements GameRatingService {

    private final UserInfoMapper userInfoMapper;
    private final OrderService orderService;
    private final RedisCacheService redisCacheService;

    /**
     * 完成游戏评价中的 createRating 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameRating gameRating 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createRating(GameRating gameRating) {
        save(gameRating);
        invalidateRatingCaches(gameRating.getGameId());
    }

    /**
     * 完成游戏评价中的 deleteRating 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteRating(Long id) {
        GameRating rating = getById(id);
        removeById(id);
        invalidateRatingCaches(rating == null ? null : rating.getGameId());
    }

    /**
     * 完成游戏评价中的 updateRating 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameRating gameRating 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateRating(GameRating gameRating) {
        updateById(gameRating);
        invalidateRatingCaches(gameRating.getGameId());
    }

    /**
     * 提交已购用户的游戏评价，包含评分、好评或差评态度。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param rating rating 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @param recommend 评价筛选标记，用来区分好评、差评或全部评价。
     * @return true 表示游戏评价当前状态满足业务判断。
     */
    @Override
    public boolean rateGame(Integer userId, Integer gameId, Integer rating, String content, Boolean recommend) {
        int buyCount = orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getGameId, gameId)
                .eq(Order::getStatus, "购买成功"));
        if (buyCount <= 0) {
            /**
             * 完成游戏评价中的 IllegalArgumentException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 游戏评价在该步骤产出的业务结果。
             */
            throw new IllegalArgumentException("购买游戏后才能评价");
        }
        if (rating == null || rating < 1 || rating > 10) {
            /**
             * 完成游戏评价中的 IllegalArgumentException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 游戏评价在该步骤产出的业务结果。
             */
            throw new IllegalArgumentException("评分必须在1到10之间");
        }
        // 检查是否已经评分过
        GameRating existingRating = getUserGameRating(userId, gameId);

        if (existingRating != null) {
            // 更新已有评分
            existingRating.setRating(rating);
            existingRating.setContent(content);
            existingRating.setRecommend(recommend);
            existingRating.setUpdateTime(LocalDateTime.now());
            boolean updated = updateById(existingRating);
            invalidateRatingCaches(gameId);
            return updated;
        } else {
            // 创建新评分
            GameRating newRating = new GameRating();
            newRating.setUserId(userId);
            newRating.setGameId(gameId);
            newRating.setRating(rating);
            newRating.setContent(content);
            newRating.setRecommend(recommend);
            newRating.setCreateTime(LocalDateTime.now());
            newRating.setUpdateTime(LocalDateTime.now());
            boolean saved = save(newRating);
            invalidateRatingCaches(gameId);
            return saved;
        }
    }

    /**
     * 读取游戏评价的 UserGameRating 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价在该步骤产出的业务结果。
     */
    @Override
    public GameRating getUserGameRating(Integer userId, Integer gameId) {
        LambdaQueryWrapper<GameRating> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GameRating::getUserId, userId)
                   .eq(GameRating::getGameId, gameId);
        return getOne(queryWrapper);
    }

    /**
     * 读取游戏评价的 GameAverageRating 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价在该步骤产出的业务结果。
     */
    @Override
    public double getGameAverageRating(Integer gameId) {
        LambdaQueryWrapper<GameRating> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GameRating::getGameId, gameId);
        List<GameRating> ratings = list(queryWrapper);

        if (ratings.isEmpty()) {
            return 0.0;
        }

        double sum = ratings.stream()
                          .mapToInt(GameRating::getRating)
                          .sum();
        return sum / ratings.size();
    }

    /**
     * 读取游戏评价的 GameRatingCount 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价统计值或主键结果。
     */
    @Override
    public int getGameRatingCount(Integer gameId) {
        LambdaQueryWrapper<GameRating> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GameRating::getGameId, gameId);
        return count(queryWrapper);
    }

    /**
     * 读取游戏评价的 GameRatingList 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏评价列表数据。
     */
    @Override
    public List<Map<String, Object>> getGameRatingList(Integer gameId) {
        return getGameRatingPage(gameId, 1, Integer.MAX_VALUE, null, null, null).getList();
    }

    /**
     * 读取游戏评价的 GameRatingPage 数据，供页面展示或后续业务判断。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @param recommend 评价筛选标记，用来区分好评、差评或全部评价。
     * @param startTime 评价发布时间起点，空值表示不限制开始时间。
     * @param endTime 评价发布时间终点，空值表示不限制结束时间。
     * @return 游戏评价分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<Map<String, Object>> getGameRatingPage(Integer gameId, Integer pageNo, Integer pageSize, Boolean recommend,
                                                             LocalDateTime startTime, LocalDateTime endTime) {
        Page<GameRating> page = page(
                new Page<>(pageNo == null || pageNo < 1 ? 1 : pageNo, pageSize == null ? 50 : pageSize),
                new LambdaQueryWrapper<GameRating>()
                        .eq(GameRating::getGameId, gameId)
                        .eq(recommend != null, GameRating::getRecommend, recommend)
                        .ge(startTime != null, GameRating::getCreateTime, startTime)
                        .lt(endTime != null, GameRating::getCreateTime, endTime)
                        .orderByDesc(GameRating::getCreateTime));
        return new PageResult<>(buildRatingRows(page.getRecords()), page.getTotal());
    }

    /**
     * 组装游戏评价所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param ratings ratings 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏评价列表数据。
     */
    private List<Map<String, Object>> buildRatingRows(List<GameRating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, UserInfo> userInfoMap = loadUserInfoMap(ratings);
        return ratings.stream()
                .map(rating -> buildRatingRow(rating, userInfoMap.get(rating.getUserId())))
                .collect(Collectors.toList());
    }

    /**
     * 完成游戏评价中的 loadUserInfoMap 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ratings ratings 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏评价聚合数据，键名与前端展示字段保持一致。
     */
    private Map<Integer, UserInfo> loadUserInfoMap(List<GameRating> ratings) {
        Set<Integer> userIds = ratings.stream()
                .map(GameRating::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userInfoMapper.selectList(new LambdaQueryWrapper<UserInfo>().in(UserInfo::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserInfo::getUserId, item -> item, (first, second) -> first));
    }

    /**
     * 组装游戏评价所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param rating rating 字段，来源于当前接口入参或内部调用上下文。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏评价聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildRatingRow(GameRating rating, UserInfo userInfo) {
        Map<String, Object> ratingInfo = new HashMap<>();
        ratingInfo.put("id", rating.getId());
        ratingInfo.put("rating", rating.getRating());
        ratingInfo.put("content", rating.getContent());
        ratingInfo.put("recommend", rating.getRecommend());
        ratingInfo.put("createTime", rating.getCreateTime() == null
                ? ""
                : rating.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        ratingInfo.put("nickname", userInfo == null ? "未知用户" : userInfo.getNickname());
        ratingInfo.put("userAvatar", userInfo == null ? "" : userInfo.getAvatar());
        return ratingInfo;
    }

    /**
     * 完成游戏评价中的 invalidateRatingCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    private void invalidateRatingCaches(Integer gameId) {
        if (gameId != null) {
            redisCacheService.delayedDoubleDelete(
                    CacheKeys.ratingAverage(gameId),
                    CacheKeys.ratingCount(gameId),
                    CacheKeys.ratingList(gameId),
                    CacheKeys.gameStats(gameId)
            );
        }
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.GAME_PATTERN, CacheKeys.RATING_PATTERN, CacheKeys.RANKING_PATTERN);
    }
}
