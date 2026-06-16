package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.GameRating;
import com.sys.pro.service.GameRatingService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 游戏评价接口控制器。
 *
 * <p>该控制器只负责接收游戏评价相关请求、做轻量参数归一化，并调用业务层完成评价提交、
 * 评价统计和评价分页查询。详情页依赖这里的公开查询接口展示评分、评价数量和玩家评价列表。</p>
 */
@Api(tags = "游戏评价接口")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/gameRating")
public class GameRatingController extends BaseController {

    private static final long RATING_CACHE_SECONDS = 180;
    private static final Duration RATING_CACHE_LOCK_TTL = Duration.ofSeconds(8);

    private final GameRatingService gameRatingService;
    private final RedisCacheService redisCacheService;

    /**
     * 根据评价主键读取单条游戏评价，供后台查看或前台回显使用。
     *
     * @param id 评价记录主键。
     * @return 单条游戏评价信息。
     */
    @ApiOperation(value = "根据id查询评价")
    @GetMapping("/get/{id}")
    public CommonResult<GameRating> getById(@PathVariable("id") Long id) {
        return CommonResult.success(gameRatingService.getById(id));
    }

    /**
     * 新增游戏评价记录，常用于后台补录或内部管理场景。
     *
     * @param gameRating 前端提交的游戏评价信息。
     * @return 新增结果。
     */
    @ApiOperation(value = "新增评价")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody GameRating gameRating) {
        gameRatingService.createRating(gameRating);
        return CommonResult.success();
    }

    /**
     * 删除指定评价，并同步清理对应游戏的评价统计缓存。
     *
     * @param id 评价记录主键。
     * @return 删除结果。
     */
    @ApiOperation(value = "删除评价")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        gameRatingService.deleteRating(id);
        return CommonResult.success();
    }

    /**
     * 更新评价内容，并同步刷新该游戏的评分统计缓存。
     *
     * @param gameRating 修改后的游戏评价信息。
     * @return 更新结果。
     */
    @ApiOperation(value = "更新评价")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody GameRating gameRating) {
        gameRatingService.updateRating(gameRating);
        return CommonResult.success();
    }

    /**
     * 提交当前登录用户对已购买游戏的评分和评价内容。
     *
     * @param gameRating 前端提交的评分、评价内容和好评/差评标识。
     * @return true 表示评价提交或更新成功。
     */
    @ApiOperation(value = "用户评价游戏")
    @PostMapping("/rate")
    public CommonResult<Boolean> rateGame(@RequestBody GameRating gameRating) {
        Integer userId = getUserId();
        try {
            boolean success = gameRatingService.rateGame(
                    userId,
                    gameRating.getGameId(),
                    gameRating.getRating(),
                    gameRating.getContent(),
                    gameRating.getRecommend());
            return CommonResult.success(success);
        } catch (IllegalArgumentException e) {
            return CommonResult.error(500, e.getMessage());
        }
    }

    /**
     * 查询指定游戏的平均评分。
     *
     * <p>该接口是游戏详情页评分展示的核心接口，使用 Redis 缓存和分布式锁减少热门游戏详情页的重复统计。</p>
     *
     * @param gameId 游戏主键。
     * @return 游戏平均评分；暂无评价时返回 0。
     */
    @ApiOperation(value = "查询游戏平均评分")
    @GetMapping("/average-rating/{gameId}")
    public CommonResult<Double> getAverageRating(@PathVariable("gameId") Integer gameId) {
        Double rating = redisCacheService.getOrLoadWithLock(
                CacheKeys.ratingAverage(gameId),
                CacheKeys.ratingAverage(gameId) + ":lock",
                RATING_CACHE_LOCK_TTL,
                120,
                RATING_CACHE_SECONDS,
                () -> gameRatingService.getGameAverageRating(gameId));
        return CommonResult.success(rating);
    }

    /**
     * 查询指定游戏的评价总数。
     *
     * <p>前台会结合评价数量和好评率计算“好评如潮”“褒贬不一”等评测标签。</p>
     *
     * @param gameId 游戏主键。
     * @return 当前游戏的评价数量。
     */
    @ApiOperation(value = "查询游戏评价数量")
    @GetMapping("/rating-count/{gameId}")
    public CommonResult<Integer> getRatingCount(@PathVariable("gameId") Integer gameId) {
        Integer count = redisCacheService.getOrLoadWithLock(
                CacheKeys.ratingCount(gameId),
                CacheKeys.ratingCount(gameId) + ":lock",
                RATING_CACHE_LOCK_TTL,
                120,
                RATING_CACHE_SECONDS,
                () -> gameRatingService.getGameRatingCount(gameId));
        return CommonResult.success(count);
    }

    /**
     * 分页查询玩家评价列表，并支持按好评或差评筛选。
     *
     * @param gameId 游戏主键。
     * @param pageNo 当前页码，默认第一页。
     * @param pageSize 每页数量，只允许 20、50、100 三档。
     * @param recommend 评价类型；true 为好评，false 为差评，空值为全部。
     * @param startTime 评价发布时间起点，空值表示不限制开始时间。
     * @param endTime 评价发布时间终点，空值表示不限制结束时间。
     * @return 玩家评价分页结果。
     */
    @ApiOperation(value = "分页查询游戏评价")
    @GetMapping("/list/{gameId}")
    public CommonResult<PageResult<Map<String, Object>>> getRatingList(@PathVariable("gameId") Integer gameId,
                                                                        @RequestParam(defaultValue = "1") Integer pageNo,
                                                                        @RequestParam(defaultValue = "50") Integer pageSize,
                                                                        @RequestParam(required = false) Boolean recommend,
                                                                        @RequestParam(required = false)
                                                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                                        @RequestParam(required = false)
                                                                        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Integer normalizedPageSize = normalizePageSize(pageSize);
        PageResult<Map<String, Object>> page = redisCacheService.getOrLoadWithLock(
                CacheKeys.ratingList(gameId, pageNo, normalizedPageSize, recommend,
                        startTime == null ? null : startTime.toString(),
                        endTime == null ? null : endTime.toString()),
                CacheKeys.ratingList(gameId) + ":lock",
                RATING_CACHE_LOCK_TTL,
                120,
                RATING_CACHE_SECONDS,
                () -> gameRatingService.getGameRatingPage(gameId, pageNo, normalizedPageSize, recommend, startTime, endTime));
        return CommonResult.success(page);
    }

    /**
     * 查询当前登录用户对某个游戏的历史评价。
     *
     * <p>如果用户已经评价过，前台会使用该数据回填评分和评价内容，便于用户继续修改。</p>
     *
     * @param gameId 游戏主键。
     * @return 当前用户的评价记录；没有评价时返回空。
     */
    @ApiOperation(value = "查询当前用户的游戏评价")
    @GetMapping("/user-rating/{gameId}")
    public CommonResult<GameRating> getUserRating(@PathVariable("gameId") Integer gameId) {
        return CommonResult.success(gameRatingService.getUserGameRating(getUserId(), gameId));
    }

    /**
     * 规范化玩家评价分页大小，避免前端传入过大页码拖慢游戏详情页。
     *
     * @param pageSize 前端传入的每页数量。
     * @return 合法分页大小，非法值统一回退到 50。
     */
    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return 50;
        }
        if (pageSize == 20 || pageSize == 50 || pageSize == 100) {
            return pageSize;
        }
        return 50;
    }
}
