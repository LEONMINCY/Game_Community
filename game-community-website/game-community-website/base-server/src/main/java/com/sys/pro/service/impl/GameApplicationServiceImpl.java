package com.sys.pro.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.BrowseHistory;
import com.sys.pro.pojo.Comment;
import com.sys.pro.pojo.Game;
import com.sys.pro.pojo.GameRating;
import com.sys.pro.pojo.Order;
import com.sys.pro.pojo.Post;
import com.sys.pro.service.BrowseHistoryService;
import com.sys.pro.service.CommentService;
import com.sys.pro.service.GameApplicationService;
import com.sys.pro.service.GameRatingService;
import com.sys.pro.service.GameService;
import com.sys.pro.service.OrderService;
import com.sys.pro.service.PostService;
import com.sys.pro.service.impl.RecommendationService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import com.sys.pro.vo.GameVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 游戏中心业务编排实现。
 * 负责价格展示、浏览历史、热门推荐和统计数据组装，Controller 仅保留接口适配职责。
 */
@Service
@RequiredArgsConstructor
public class GameApplicationServiceImpl implements GameApplicationService {

    private static final long SHORT_CACHE_SECONDS = 120;
    private static final long NORMAL_CACHE_SECONDS = 300;
    private static final long HOT_CACHE_SECONDS = 600;

    private final GameService gameService;
    private final OrderService orderService;
    private final BrowseHistoryService browseHistoryService;
    private final RecommendationService recommendationService;
    private final PostService postService;
    private final CommentService commentService;
    private final RedisCacheService redisCacheService;
    private final GameRatingService gameRatingService;

    /**
     * 根据主键读取游戏详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 游戏在该步骤产出的业务结果。
     */
    @Override
    public GameVo getById(Integer id, Integer userId) {
        if (userId != null) {
            BrowseHistory browseHistory = new BrowseHistory();
            browseHistory.setUserId(userId);
            browseHistory.setTargetId(id);
            browseHistory.setTargetType("game");
            browseHistoryService.save(browseHistory);
        }

        Game game = gameService.getById(id);
        GameVo gameVo = new GameVo();
        BeanUtil.copyProperties(game, gameVo);
        fillGamePrice(gameVo);
        gameVo.setIsBuy(userId != null && hasBought(userId, id));
        return gameVo;
    }

    /**
     * 接收新增游戏数据，完成入口校验后交由业务层保存。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void create(Game game) {
        normalizeGamePriceFields(game);
        gameService.save(game);
        invalidateGameCaches();
    }

    /**
     * 按主键删除游戏记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void delete(Long id) {
        List<Post> list = postService.list(new LambdaQueryWrapper<Post>().eq(Post::getGameId, id));
        if (!list.isEmpty()) {
            LambdaQueryWrapper<Post> ids = new LambdaQueryWrapper<Post>().in(Post::getId, list.stream().map(Post::getId).collect(Collectors.toList()));
            postService.remove(ids);
            commentService.remove(new LambdaQueryWrapper<Comment>().in(Comment::getPostId, ids));
        }
        browseHistoryService.remove(new LambdaQueryWrapper<BrowseHistory>().eq(BrowseHistory::getTargetId, id));
        gameService.removeById(id);
        invalidateGameAndPostCaches();
    }

    /**
     * 保存游戏编辑后的内容，让前台展示和后台管理保持一致。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void update(Game game) {
        normalizeGamePriceFields(game);
        gameService.updateById(game);
        invalidateGameCaches();
    }

    /**
     * 读取全部可用游戏数据，供下拉框或初始化页面使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 游戏列表数据。
     */
    @Override
    public List<Game> listAll(String keyword, Integer limit) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        List<Game> games = redisCacheService.getOrLoad(CacheKeys.gameListAll(searchKeyword), NORMAL_CACHE_SECONDS,
                () -> gameService.list(new LambdaQueryWrapper<Game>()
                        .select(Game::getId, Game::getName, Game::getIcon)
                        .like(StringUtils.hasText(searchKeyword), Game::getName, searchKeyword)));
        if (limit != null) {
            int size = Math.max(1, Math.min(limit, 100));
            if (games.size() > size) {
                return games.subList(0, size);
            }
        }
        return games;
    }

    /**
     * 读取游戏列表数据，按页面传入条件完成筛选和排序。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<Game> list(GameVo game) {
        String cacheKey = CacheKeys.gameList(JSON.toJSONString(game));
        return redisCacheService.getOrLoad(cacheKey, SHORT_CACHE_SECONDS, () -> listGamesFromDb(game));
    }

    /**
     * 根据用户行为和热度指标生成游戏推荐列表。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 游戏列表数据。
     */
    @Override
    public List<Game> recommend(Integer userId) {
        List<Long> gameIds = userId == null ? Collections.emptyList() : recommendationService.recommendGames(userId.longValue());
        return buildRecommendedGames(gameIds, 20);
    }

    /**
     * 按销量、好评率或业务热度读取热门游戏列表。
     * @return 游戏列表数据。
     */
    @Override
    public List<Game> listHot() {
        return redisCacheService.getOrLoad(CacheKeys.gameHot(), HOT_CACHE_SECONDS, () ->
                rankGamesByPopularity(gameService.list()).stream().limit(10).collect(Collectors.toList()));
    }

    /**
     * 按发帖量统计热门社区，供首页侧栏跳转到对应帖子列表。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 游戏列表数据。
     */
    @Override
    public List<Game> hotCommunities(Integer limit) {
        return redisCacheService.getOrLoad(CacheKeys.gameHotCommunities(limit), HOT_CACHE_SECONDS, () -> {
            List<Game> games = gameService.list();
            games.forEach(game -> {
                game.setPostCount(countApprovedPosts(game.getId()));
                fillGamePrice(game);
            });
            games.sort((a, b) -> b.getPostCount() - a.getPostCount());
            int size = Math.min(Math.max(limit == null ? 5 : limit, 1), games.size());
            return games.subList(0, size);
        });
    }

    /**
     * 读取游戏类型标签，供前台筛选和后台表单复用。
     * @return 游戏列表数据。
     */
    @Override
    public List<String> types() {
        return redisCacheService.getOrLoad(CacheKeys.gameTypes(), HOT_CACHE_SECONDS, () -> {
            List<Game> games = gameService.list(new LambdaQueryWrapper<Game>()
                    .select(Game::getType));
            return games.stream()
                    .flatMap(game -> splitTypes(game.getType()).stream())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        });
    }

    /**
     * 读取正在打折的游戏列表，展示限时优惠入口。
     * @return 游戏列表数据。
     */
    @Override
    public List<Game> discounts() {
        return redisCacheService.getOrLoad(CacheKeys.gameDiscounts(), NORMAL_CACHE_SECONDS, () -> {
            List<Game> games = gameService.list(new LambdaQueryWrapper<Game>()
                    .gt(Game::getDiscount, 0));
            games.forEach(game -> {
                decorateGameRatingSummary(game);
                fillGamePrice(game);
            });
            return games;
        });
    }

    /**
     * 汇总游戏详情页需要的评分、销量和趋势统计。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 游戏聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> stats(Integer id) {
        return redisCacheService.getOrLoad(CacheKeys.gameStats(id), NORMAL_CACHE_SECONDS, () -> buildGameStats(id));
    }

    /**
     * 汇总游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏分页结果，包含当前页数据和总数。
     */
    private PageResult<Game> listGamesFromDb(GameVo game) {
        Page<Game> page = new Page<>(game.getPageNo(), game.getPageSize());
        String name = game.getName() == null ? "" : game.getName().trim();
        String type = game.getType() == null ? "" : game.getType().trim();
        String developer = game.getDeveloper() == null ? "" : game.getDeveloper().trim();
        String reviewType = game.getReviewType() == null ? "" : game.getReviewType().trim();
        BigDecimal[] ratingRange = parseRatingRange(game.getRatingRange());

        LambdaQueryWrapper<Game> queryWrapper = new LambdaQueryWrapper<Game>()
                .like(StringUtils.hasText(name), Game::getName, name)
                .like(StringUtils.hasText(type), Game::getType, type)
                .like(StringUtils.hasText(developer), Game::getDeveloper, developer);

        if (ratingRange != null || StringUtils.hasText(reviewType)) {
            List<Game> matchedGames = decorateGames(gameService.list(queryWrapper)).stream()
                    .filter(item -> ratingRange == null || (item.getRating() != null
                            && item.getRating().compareTo(ratingRange[0]) >= 0
                            && item.getRating().compareTo(ratingRange[1]) <= 0))
                    .filter(item -> !StringUtils.hasText(reviewType) || reviewType.equals(item.getReviewType()))
                    .collect(Collectors.toList());
            int pageNo = game.getPageNo() == null ? 1 : game.getPageNo();
            int pageSize = game.getPageSize() == null ? 10 : game.getPageSize();
            int fromIndex = Math.min((pageNo - 1) * pageSize, matchedGames.size());
            int toIndex = Math.min(fromIndex + pageSize, matchedGames.size());
            return new PageResult<>(matchedGames.subList(fromIndex, toIndex), (long) matchedGames.size());
        }

        IPage<Game> resultList = gameService.page(page, queryWrapper);
        resultList.setRecords(decorateGames(resultList.getRecords()));
        return new PageResult<>(resultList);
    }

    /**
     * 完成游戏中的 decorateGames 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameList gameList 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏列表数据。
     */
    private List<Game> decorateGames(List<Game> gameList) {
        return gameList.stream().map(item -> {
            decorateGameRatingSummary(item);
            fillGamePrice(item);
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 组装游戏所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 游戏聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> buildGameStats(Integer id) {
        List<GameRating> ratings = gameRatingService.list(new LambdaQueryWrapper<GameRating>()
                .eq(GameRating::getGameId, id)
                .orderByAsc(GameRating::getCreateTime));
        List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getGameId, id)
                .eq(Order::getStatus, "购买成功")
                .orderByAsc(Order::getCreateTime));

        int ratingCount = ratings.size();
        long goodCount = ratings.stream().filter(this::isRecommendedRating).count();
        BigDecimal goodRate = ratingCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(goodCount * 100.0 / ratingCount).setScale(1, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("ratingCount", ratingCount);
        result.put("goodRate", goodRate);
        result.put("reviewType", calculateReviewType(ratingCount, goodRate));
        result.put("totalSales", orders.size());
        result.put("ratingTrend", buildRatingTrend(ratings));
        result.put("monthlySalesTrend", buildMonthlySalesTrend(orders));
        return result;
    }

    /**
     * 完成游戏中的 normalizeGamePriceFields 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */
    private void normalizeGamePriceFields(Game game) {
        game.setDiscount(normalizeDiscount(game.getDiscount()));
        game.setPriceMark(normalizePriceMark(game.getPriceMark()));
    }

    /**
     * 解析游戏相关输入，把原始字符串或请求参数转换成业务对象。
     * @param ratingRange ratingRange 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏在该步骤产出的业务结果。
     */
    private BigDecimal[] parseRatingRange(String ratingRange) {
        if (!StringUtils.hasText(ratingRange)) {
            return null;
        }
        String[] parts = ratingRange.split(",");
        try {
            BigDecimal min = parts.length > 0 && StringUtils.hasText(parts[0])
                    ? new BigDecimal(parts[0].trim())
                    : BigDecimal.ZERO;
            BigDecimal max = parts.length > 1 && StringUtils.hasText(parts[1])
                    ? new BigDecimal(parts[1].trim())
                    : BigDecimal.TEN;
            if (min.compareTo(max) > 0) {
                BigDecimal temp = min;
                min = max;
                max = temp;
            }
            return new BigDecimal[]{min, max};
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 判断游戏当前状态是否满足业务条件。
     * @param rating rating 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示游戏当前状态满足业务判断。
     */
    private boolean isRecommendedRating(GameRating rating) {
        return Boolean.TRUE.equals(rating.getRecommend());
    }

    /**
     * 为单个游戏补齐评分均值、评价数量、好评率和文字标签，保证列表页和详情页口径一致。
     * @param game 需要补充展示字段的游戏记录。
     */
    private void decorateGameRatingSummary(Game game) {
        List<GameRating> ratings = gameRatingService.list(new LambdaQueryWrapper<GameRating>()
                .eq(GameRating::getGameId, game.getId()));
        int ratingCount = ratings.size();
        long goodCount = ratings.stream().filter(this::isRecommendedRating).count();
        double average = ratings.stream()
                .map(GameRating::getRating)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0D);
        BigDecimal goodRate = ratingCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(goodCount * 100.0 / ratingCount).setScale(1, RoundingMode.HALF_UP);
        game.setRating(BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_DOWN));
        game.setReviewCount(ratingCount);
        game.setGoodRate(goodRate);
        game.setReviewType(calculateReviewType(ratingCount, goodRate));
    }

    /**
     * 按 Steam 评测规则计算展示标签，评价数和推荐率共同决定文案。
     * @param ratingCount 参与统计的评价总数。
     * @param goodRate 明确选择推荐的评价占比。
     * @return 前台展示的评价类型。
     */
    private String calculateReviewType(int ratingCount, BigDecimal goodRate) {
        if (ratingCount <= 0) {
            return "暂无用户评测";
        }
        double rate = goodRate == null ? 0D : goodRate.doubleValue();
        if (rate >= 95 && ratingCount >= 500) {
            return "好评如潮";
        }
        if ((rate >= 80 && rate <= 94 && ratingCount >= 50) || (rate >= 95 && ratingCount >= 50)) {
            return "特别好评";
        }
        if (rate >= 80) {
            return "好评";
        }
        if (rate >= 70 && ratingCount >= 10) {
            return "多半好评";
        }
        if (rate >= 40 && ratingCount >= 10) {
            return "褒贬不一";
        }
        if (rate >= 20 && ratingCount >= 10) {
            return "多半差评";
        }
        if (rate <= 19 && ratingCount >= 500) {
            return "差评如潮";
        }
        if (rate <= 19 && ratingCount >= 50) {
            return "特别差评";
        }
        if (rate <= 19) {
            return "差评";
        }
        return "褒贬不一";
    }

    /**
     * 完成游戏中的 decorateGamePopularity 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */
    private void decorateGamePopularity(Game game) {
        int salesCount = orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getGameId, game.getId())
                .eq(Order::getStatus, "购买成功"));
        List<GameRating> ratings = gameRatingService.list(new LambdaQueryWrapper<GameRating>()
                .eq(GameRating::getGameId, game.getId()));
        long goodCount = ratings.stream().filter(this::isRecommendedRating).count();
        double average = ratings.stream()
                .map(GameRating::getRating)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0D);
        BigDecimal goodRate = ratings.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(goodCount * 100.0 / ratings.size()).setScale(1, RoundingMode.HALF_UP);
        BigDecimal hotScore = BigDecimal.valueOf(salesCount).multiply(BigDecimal.TEN).add(goodRate);
        game.setRating(BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_DOWN));
        game.setSalesCount(salesCount);
        game.setGoodRate(goodRate);
        game.setReviewCount(ratings.size());
        game.setReviewType(calculateReviewType(ratings.size(), goodRate));
        game.setHotScore(hotScore);
        game.setPostCount(countApprovedPosts(game.getId()));
        fillGamePrice(game);
    }

    /**
     * 组装游戏所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 游戏列表数据。
     */
    private List<Game> buildRecommendedGames(List<Long> gameIds, int limit) {
        int size = Math.max(1, limit);
        if (gameIds == null || gameIds.isEmpty()) {
            return rankGamesByPopularity(gameService.list()).stream().limit(size).collect(Collectors.toList());
        }

        Map<Integer, Integer> priority = new HashMap<>();
        for (int i = 0; i < gameIds.size(); i++) {
            priority.put(gameIds.get(i).intValue(), i);
        }
        Set<Integer> priorityIds = new HashSet<>(priority.keySet());
        List<Game> recommended = new ArrayList<>(gameService.listByIds(priorityIds));
        recommended.forEach(this::decorateGamePopularity);
        recommended.sort(Comparator
                .comparingInt((Game game) -> priority.getOrDefault(game.getId(), Integer.MAX_VALUE))
                .thenComparing(Game::getHotScore, Comparator.nullsLast(Comparator.reverseOrder())));

        if (recommended.size() < size) {
            List<Game> fallback = rankGamesByPopularity(gameService.list()).stream()
                    .filter(game -> !priorityIds.contains(game.getId()))
                    .limit(size - recommended.size())
                    .collect(Collectors.toList());
            recommended.addAll(fallback);
        }
        return recommended.stream().limit(size).collect(Collectors.toList());
    }

    /**
     * 完成游戏中的 rankGamesByPopularity 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param games games 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏列表数据。
     */
    private List<Game> rankGamesByPopularity(List<Game> games) {
        games.forEach(this::decorateGamePopularity);
        games.sort(Comparator
                .comparing(Game::getHotScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Game::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return games;
    }

    /**
     * 完成游戏中的 countApprovedPosts 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏统计值或主键结果。
     */
    private int countApprovedPosts(Integer gameId) {
        return postService.count(new LambdaQueryWrapper<Post>()
                .eq(Post::getGameId, gameId)
                .eq(Post::getDeleted, false)
                .and(wrapper -> wrapper.eq(Post::getAuditStatus, "approved")
                        .or()
                        .isNull(Post::getAuditStatus)
                        .or()
                        .eq(Post::getAuditStatus, "")));
    }

    /**
     * 完成游戏中的 hasBought 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示游戏当前状态满足业务判断。
     */
    private boolean hasBought(Integer userId, Integer gameId) {
        return orderService.count(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getGameId, gameId)
                .eq(Order::getStatus, "购买成功")) > 0;
    }

    /**
     * 完成游戏中的 fillGamePrice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */
    private void fillGamePrice(Game game) {
        if (game == null || game.getPrice() == null) {
            return;
        }
        int discount = normalizeDiscount(game.getDiscount());
        game.setDiscount(discount);
        game.setFinalPrice(calculateFinalPrice(game.getPrice(), discount));
        game.setOriginalPrice(discount > 0 ? game.getPrice() : null);
    }

    /**
     * 完成游戏中的 fillGamePrice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     */
    private void fillGamePrice(GameVo game) {
        if (game == null || game.getPrice() == null) {
            return;
        }
        int discount = normalizeDiscount(game.getDiscount());
        game.setDiscount(discount);
        game.setFinalPrice(calculateFinalPrice(game.getPrice(), discount));
        game.setOriginalPrice(discount > 0 ? game.getPrice() : null);
    }

    /**
     * 完成游戏中的 normalizeDiscount 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param discount discount 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏统计值或主键结果。
     */
    private int normalizeDiscount(Integer discount) {
        if (discount == null) {
            return 0;
        }
        if (discount < 0) {
            return 0;
        }
        return Math.min(discount, 100);
    }

    /**
     * 完成游戏中的 normalizePriceMark 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param priceMark priceMark 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏处理后的文本结果。
     */
    private String normalizePriceMark(String priceMark) {
        if (!StringUtils.hasText(priceMark)) {
            return null;
        }
        String value = priceMark.trim();
        if ("historical_low".equals(value) || "tie_historical_low".equals(value)) {
            return value;
        }
        return null;
    }

    /**
     * 完成游戏中的 calculateFinalPrice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param price price 字段，来源于当前接口入参或内部调用上下文。
     * @param discount discount 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏统计值或主键结果。
     */
    private int calculateFinalPrice(Integer price, int discount) {
        if (discount <= 0) {
            return price;
        }
        return BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 完成游戏中的 invalidateGameCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     */
    private void invalidateGameCaches() {
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.GAME_PATTERN, CacheKeys.RATING_PATTERN, CacheKeys.RANKING_PATTERN);
    }

    /**
     * 完成游戏中的 invalidateGameAndPostCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     */
    private void invalidateGameAndPostCaches() {
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.GAME_PATTERN, CacheKeys.RATING_PATTERN, CacheKeys.POST_PATTERN, CacheKeys.RANKING_PATTERN);
    }

    /**
     * 完成游戏中的 splitTypes 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏列表数据。
     */
    private List<String> splitTypes(String type) {
        if (!StringUtils.hasLength(type)) {
            return Collections.emptyList();
        }
        String[] parts = type.split("[,，、/|]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (StringUtils.hasLength(value)) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 组装游戏所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param ratings ratings 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏列表数据。
     */
    private List<Map<String, Object>> buildRatingTrend(List<GameRating> ratings) {
        Map<String, List<GameRating>> grouped = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        ratings.stream()
                .filter(rating -> rating.getCreateTime() != null)
                .forEach(rating -> grouped.computeIfAbsent(rating.getCreateTime().format(formatter), key -> new ArrayList<>()).add(rating));

        List<Map<String, Object>> trend = new ArrayList<>();
        grouped.forEach((month, monthRatings) -> {
            double avg = monthRatings.stream().mapToInt(GameRating::getRating).average().orElse(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("count", monthRatings.size());
            item.put("avgRating", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            trend.add(item);
        });
        return trend;
    }

    /**
     * 组装游戏所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param orders orders 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏列表数据。
     */
    private List<Map<String, Object>> buildMonthlySalesTrend(List<Order> orders) {
        Map<String, Integer> grouped = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        orders.stream()
                .filter(order -> order.getCreateTime() != null)
                .forEach(order -> grouped.merge(order.getCreateTime().format(formatter), 1, Integer::sum));

        List<Map<String, Object>> trend = new ArrayList<>();
        grouped.forEach((month, sales) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", month);
            item.put("sales", sales);
            trend.add(item);
        });
        return trend;
    }
}
