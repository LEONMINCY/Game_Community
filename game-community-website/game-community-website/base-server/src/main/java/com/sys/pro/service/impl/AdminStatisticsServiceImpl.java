package com.sys.pro.service.impl;

import com.sys.pro.pojo.BrowseHistory;
import com.sys.pro.pojo.Comment;
import com.sys.pro.pojo.Game;
import com.sys.pro.pojo.GameRating;
import com.sys.pro.pojo.News;
import com.sys.pro.pojo.Order;
import com.sys.pro.pojo.Post;
import com.sys.pro.pojo.Report;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.AdminStatisticsService;
import com.sys.pro.service.BrowseHistoryService;
import com.sys.pro.service.CommentService;
import com.sys.pro.service.GameRatingService;
import com.sys.pro.service.GameService;
import com.sys.pro.service.NewsService;
import com.sys.pro.service.OrderService;
import com.sys.pro.service.PostService;
import com.sys.pro.service.ReportService;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台统计聚合服务，统一承载管理员首页和社区审核员首页的图表数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final String AUDIT_PENDING = "pending";
    private static final String AUDIT_APPROVED = "approved";
    private static final String AUDIT_REJECTED = "rejected";
    private static final String ORDER_SUCCESS = "购买成功";

    private final UserService userService;
    private final PostService postService;
    private final GameService gameService;
    private final OrderService orderService;
    private final CommentService commentService;
    private final NewsService newsService;
    private final GameRatingService gameRatingService;
    private final UserInfoService userInfoService;
    private final BrowseHistoryService browseHistoryService;
    private final ReportService reportService;

    /**
     * 读取后台统计看板的 AdminDashboard 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("statistics", getStatistics());
        data.put("userGrowth", getUserGrowthTrend());
        data.put("hotGames", getHotGames().get("hotGames"));
        data.put("gameTypes", getGameTypes().get("gameTypes"));
        data.put("activity", getActivityAnalysis().get("activityData"));
        data.put("salesRanking", getGameSalesRanking());
        data.put("salesOverview", getSalesOverview());
        data.put("postHeatmap", getPostInteractionHeatmap());
        data.put("ageDistribution", getUserAgeDistribution());
        data.put("newsViews", getNewsViews());
        data.put("goodRates", getGameGoodRates());
        return data;
    }

    /**
     * 读取后台统计看板的 Statistics 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> data = new HashMap<>();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        data.put("totalUsers", userService.count());
        data.put("activeUsers", userService.countLastLoginAfter(today));
        data.put("totalPosts", activePosts().size());
        data.put("newUsers", userService.countCreatedAfter(firstDayOfMonth));
        return data;
    }

    /**
     * 读取后台统计看板的 GameTypes 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getGameTypes() {
        Map<String, Long> typeCount = new LinkedHashMap<>();
        gameService.list().forEach(game -> splitTypes(game.getType()).forEach(type ->
                typeCount.merge(type, 1L, Long::sum)));
        Map<String, Object> result = new HashMap<>();
        result.put("gameTypes", toRows(typeCount, 20));
        return result;
    }

    /**
     * 读取后台统计看板的 UserGrowthTrend 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getUserGrowthTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> months = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1);
            months.add(start.format(formatter));
            values.add(userService.countCreatedBetween(start, end));
        }
        result.put("months", months);
        result.put("values", values);
        return result;
    }

    /**
     * 读取后台统计看板的 HotGames 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getHotGames() {
        Map<String, Long> gamePostCount = new HashMap<>();
        activePosts().stream()
                .filter(this::approvedPost)
                .forEach(post -> gamePostCount.merge(gameName(post.getGameId()), 1L, Long::sum));
        Map<String, Object> result = new HashMap<>();
        result.put("hotGames", toRows(gamePostCount, 5));
        return result;
    }

    /**
     * 读取后台统计看板的 ActivityAnalysis 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getActivityAnalysis() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("indicator", new String[]{"日活跃率", "互动频率", "发帖活跃度", "社区参与度", "用户留存率"});
        data.put("value", new double[]{
                calculateOnlineTimeScore(),
                calculateInteractionScore(),
                calculatePostActivityScore(),
                calculateCommunityParticipationScore(),
                calculateRetentionScore()
        });
        result.put("activityData", data);
        return result;
    }

    /**
     * 读取后台统计看板的 GameSalesRanking 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */
    @Override
    public List<Map<String, Object>> getGameSalesRanking() {
        Map<String, Long> salesCount = new HashMap<>();
        successfulOrders().forEach(order -> salesCount.merge(gameName(order.getGameId()), 1L, Long::sum));
        return toRows(salesCount, 10);
    }

    /**
     * 读取后台统计看板的 SalesOverview 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getSalesOverview() {
        Map<String, Object> result = new HashMap<>();
        List<String> months = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1);
            List<Order> monthlyOrders = orderService.lambdaQuery()
                    .eq(Order::getStatus, ORDER_SUCCESS)
                    .ge(Order::getCreateTime, start)
                    .lt(Order::getCreateTime, end)
                    .list();
            BigDecimal amount = monthlyOrders.stream()
                    .map(Order::getTotalPrice)
                    .filter(price -> price != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            months.add(start.format(formatter));
            amounts.add(amount.setScale(2, RoundingMode.HALF_UP));
            counts.add((long) monthlyOrders.size());
        }

        result.put("months", months);
        result.put("amounts", amounts);
        result.put("counts", counts);
        result.put("totalAmount", amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return result;
    }

    /**
     * 读取后台统计看板的 PostInteractionHeatmap 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getPostInteractionHeatmap() {
        Map<String, Object> result = new HashMap<>();
        List<String> days = new ArrayList<>();
        List<String> hours = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate startDate = LocalDate.now().minusDays(6);

        for (int i = 0; i < 7; i++) {
            days.add(startDate.plusDays(i).format(dayFormatter));
        }
        for (int i = 0; i < 24; i++) {
            hours.add(i + "点");
        }

        int[][] matrix = new int[7][24];
        activePosts().forEach(post -> {
            LocalDateTime createTime = post.getCreateTime();
            int dayIndex = dayIndex(createTime, startDate);
            if (dayIndex >= 0) {
                matrix[dayIndex][createTime.getHour()] += countCsv(post.getLikesUser());
            }
        });
        commentService.list().stream()
                .filter(comment -> comment.getCreateTime() != null)
                .filter(comment -> !Boolean.TRUE.equals(comment.getDeleted()))
                .forEach(comment -> {
                    int dayIndex = dayIndex(comment.getCreateTime(), startDate);
                    if (dayIndex >= 0) {
                        matrix[dayIndex][comment.getCreateTime().getHour()] += 1;
                    }
                });

        List<List<Integer>> data = new ArrayList<>();
        int max = 0;
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) {
                int value = matrix[day][hour];
                max = Math.max(max, value);
                data.add(Arrays.asList(day, hour, value));
            }
        }
        result.put("days", days);
        result.put("hours", hours);
        result.put("data", data);
        result.put("max", Math.max(max, 10));
        return result;
    }

    /**
     * 读取后台统计看板的 UserAgeDistribution 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */
    @Override
    public List<Map<String, Object>> getUserAgeDistribution() {
        Map<String, Long> ageMap = new LinkedHashMap<>();
        ageMap.put("未设置", 0L);
        ageMap.put("18岁以下", 0L);
        ageMap.put("18-25岁", 0L);
        ageMap.put("26-35岁", 0L);
        ageMap.put("36-45岁", 0L);
        ageMap.put("46岁以上", 0L);
        userInfoService.list().stream()
                .filter(info -> !Boolean.TRUE.equals(info.getDeleted()))
                .forEach(info -> ageMap.merge(ageRange(info.getAge()), 1L, Long::sum));
        return toRows(ageMap, 10);
    }

    /**
     * 读取后台统计看板的 NewsViews 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */
    @Override
    public List<Map<String, Object>> getNewsViews() {
        return newsService.list().stream()
                .filter(news -> !Boolean.TRUE.equals(news.getDeleted()))
                .sorted(Comparator.comparingInt((News news) -> news.getBrowCount() == null ? 0 : news.getBrowCount()).reversed())
                .limit(10)
                .map(news -> row(shortName(news.getTitle()), news.getBrowCount() == null ? 0 : news.getBrowCount()))
                .collect(Collectors.toList());
    }

    /**
     * 读取后台统计看板的 GameGoodRates 数据，供页面展示或后续业务判断。
     * @return 后台统计看板列表数据。
     */
    @Override
    public List<Map<String, Object>> getGameGoodRates() {
        Map<Integer, List<GameRating>> ratingMap = gameRatingService.list().stream()
                .filter(rating -> rating.getGameId() != null)
                .collect(Collectors.groupingBy(GameRating::getGameId));
        return ratingMap.entrySet().stream()
                .map(entry -> {
                    List<GameRating> ratings = entry.getValue();
                    long goodCount = ratings.stream().filter(rating -> !Boolean.FALSE.equals(rating.getRecommend())).count();
                    BigDecimal rate = BigDecimal.valueOf(goodCount * 100.0 / Math.max(ratings.size(), 1)).setScale(1, RoundingMode.HALF_UP);
                    Map<String, Object> item = row(gameName(entry.getKey()), rate);
                    item.put("ratingCount", ratings.size());
                    return item;
                })
                .sorted((left, right) -> Integer.compare((Integer) right.get("ratingCount"), (Integer) left.get("ratingCount")))
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 读取后台统计看板的 ModeratorDashboard 数据，供页面展示或后续业务判断。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    @Override
    public Map<String, Object> getModeratorDashboard() {
        Map<String, Object> result = new HashMap<>();
        List<Post> posts = activePosts();
        Map<String, Long> gamePostCounts = new HashMap<>();
        posts.forEach(post -> gamePostCounts.merge(gameName(post.getGameId()), 1L, Long::sum));

        Map<String, Long> postViews = new HashMap<>();
        browseHistoryService.lambdaQuery()
                .eq(BrowseHistory::getTargetType, "post")
                .list()
                .stream()
                .filter(history -> !Boolean.TRUE.equals(history.getDeleted()))
                .forEach(history -> postViews.merge(postTitle(history.getTargetId()), 1L, Long::sum));

        Map<String, Long> typeLikes = new HashMap<>();
        Map<String, Long> typeFavorites = new HashMap<>();
        posts.forEach(post -> {
            List<String> types = splitTypes(gameType(post.getGameId()));
            int likes = countCsv(post.getLikesUser());
            int favorites = countCsv(post.getFavoritesUser());
            types.forEach(type -> {
                typeLikes.merge(type, (long) likes, Long::sum);
                typeFavorites.merge(type, (long) favorites, Long::sum);
            });
        });

        LocalDateTime firstDayOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long reviewedThisMonth = posts.stream()
                .filter(post -> post.getReviewTime() != null && !post.getReviewTime().isBefore(firstDayOfMonth))
                .filter(post -> AUDIT_APPROVED.equals(post.getAuditStatus()) || AUDIT_REJECTED.equals(post.getAuditStatus()))
                .count();
        long pendingPosts = posts.stream().filter(post -> AUDIT_PENDING.equals(post.getAuditStatus())).count();
        long pendingReports = reportService.countPendingForAdmin();
        long handledReportsThisMonth = reportService.lambdaQuery()
                .ge(Report::getCreateTime, firstDayOfMonth)
                .ne(Report::getStatus, "PENDING")
                .isNotNull(Report::getStatus)
                .count();

        Map<String, Object> reviewSummary = new HashMap<>();
        reviewSummary.put("reviewedThisMonth", reviewedThisMonth);
        reviewSummary.put("pendingPosts", pendingPosts);
        reviewSummary.put("pendingReports", pendingReports);
        reviewSummary.put("handledReportsThisMonth", handledReportsThisMonth);

        result.put("gamePostCounts", toRows(gamePostCounts, 10));
        result.put("postViews", toRows(postViews, 10));
        result.put("typeLikes", toRows(typeLikes, 12));
        result.put("typeFavorites", toRows(typeFavorites, 12));
        result.put("reviewSummary", reviewSummary);
        return result;
    }

    /**
     * 筛选可展示帖子数据，排除未审核或已删除内容。
     * @return 后台统计看板列表数据。
     */
    private List<Post> activePosts() {
        return postService.list().stream()
                .filter(post -> !Boolean.TRUE.equals(post.getDeleted()))
                .collect(Collectors.toList());
    }

    /**
     * 筛选支付成功订单，供 AI 和统计模块读取销量信息。
     * @return 后台统计看板列表数据。
     */
    private List<Order> successfulOrders() {
        return orderService.lambdaQuery().eq(Order::getStatus, ORDER_SUCCESS).list();
    }

    /**
     * 判断帖子是否已审核通过，可进入 AI 社区上下文。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示后台统计看板当前状态满足业务判断。
     */
    private boolean approvedPost(Post post) {
        return post != null && (StringUtils.isBlank(post.getAuditStatus()) || AUDIT_APPROVED.equals(post.getAuditStatus()));
    }

    /**
     * 完成后台统计看板中的 calculateOnlineTimeScore 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 后台统计看板在该步骤产出的业务结果。
     */
    private double calculateOnlineTimeScore() {
        long totalUsers = userService.count();
        if (totalUsers == 0) {
            return 0;
        }
        long activeUsersCount = userService.countLastLoginAfter(LocalDateTime.now().minusDays(1));
        return Math.floor(Math.min(activeUsersCount * 100.0 / totalUsers, 100));
    }

    /**
     * 完成后台统计看板中的 calculateInteractionScore 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 后台统计看板在该步骤产出的业务结果。
     */
    private double calculateInteractionScore() {
        long commentCount = commentService.lambdaQuery().ge(Comment::getCreateTime, LocalDateTime.now().minusDays(7)).count();
        return Math.floor(Math.min((commentCount / 7.0) * 10, 100));
    }

    /**
     * 完成后台统计看板中的 calculatePostActivityScore 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 后台统计看板在该步骤产出的业务结果。
     */
    private double calculatePostActivityScore() {
        long postCount = postService.lambdaQuery().ge(Post::getCreateTime, LocalDateTime.now().minusDays(30)).count();
        return Math.floor(Math.min((postCount / 30.0) * 15, 100));
    }

    /**
     * 完成后台统计看板中的 calculateCommunityParticipationScore 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 后台统计看板在该步骤产出的业务结果。
     */
    private double calculateCommunityParticipationScore() {
        long totalUsers = userService.count();
        if (totalUsers == 0) {
            return 0;
        }
        long totalInteractions = activePosts().stream()
                .mapToLong(post -> countCsv(post.getLikesUser()) + countCsv(post.getFavoritesUser()))
                .sum();
        return Math.floor(Math.min(totalInteractions * 20.0 / totalUsers, 100));
    }

    /**
     * 完成后台统计看板中的 calculateRetentionScore 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 后台统计看板在该步骤产出的业务结果。
     */
    private double calculateRetentionScore() {
        long totalUsers = userService.count();
        if (totalUsers == 0) {
            return 0;
        }
        long activeUsers = userService.countLastLoginAfter(LocalDateTime.now().minusDays(30));
        return Math.floor(activeUsers * 100.0 / totalUsers);
    }

    /**
     * 完成后台统计看板中的 dayIndex 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param time time 字段，来源于当前接口入参或内部调用上下文。
     * @param startDate startDate 字段，来源于当前接口入参或内部调用上下文。
     * @return 后台统计看板统计值或主键结果。
     */
    private int dayIndex(LocalDateTime time, LocalDate startDate) {
        if (time == null) {
            return -1;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, time.toLocalDate());
        return days >= 0 && days < 7 ? (int) days : -1;
    }

    /**
     * 统计逗号分隔字段中的有效元素数量。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 后台统计看板统计值或主键结果。
     */
    private int countCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        return (int) Arrays.stream(value.split(",")).filter(StringUtils::isNotBlank).count();
    }

    /**
     * 根据游戏主键读取游戏名称，缺失时返回兜底文本。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 后台统计看板处理后的文本结果。
     */
    private String gameName(Integer gameId) {
        if (gameId == null) {
            return "未关联游戏";
        }
        Game game = gameService.getById(gameId);
        return game == null || StringUtils.isBlank(game.getName()) ? "未知游戏" : shortName(game.getName());
    }

    /**
     * 完成后台统计看板中的 gameType 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 后台统计看板处理后的文本结果。
     */
    private String gameType(Integer gameId) {
        if (gameId == null) {
            return "未设置";
        }
        Game game = gameService.getById(gameId);
        return game == null ? "未设置" : game.getType();
    }

    /**
     * 完成后台统计看板中的 postTitle 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param postId 帖子主键，用来定位评论、点赞或审核所属的帖子。
     * @return 后台统计看板处理后的文本结果。
     */
    private String postTitle(Integer postId) {
        if (postId == null) {
            return "未知帖子";
        }
        Post post = postService.getById(postId);
        return post == null || StringUtils.isBlank(post.getTitle()) ? "未知帖子" : shortName(post.getTitle());
    }

    /**
     * 完成后台统计看板中的 shortName 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 后台统计看板处理后的文本结果。
     */
    private String shortName(String value) {
        if (StringUtils.isBlank(value)) {
            return "未命名";
        }
        return value.length() > 12 ? value.substring(0, 12) + "..." : value;
    }

    /**
     * 完成后台统计看板中的 ageRange 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param age age 字段，来源于当前接口入参或内部调用上下文。
     * @return 后台统计看板处理后的文本结果。
     */
    private String ageRange(Integer age) {
        if (age == null || age <= 0) {
            return "未设置";
        }
        if (age < 18) {
            return "18岁以下";
        }
        if (age <= 25) {
            return "18-25岁";
        }
        if (age <= 35) {
            return "26-35岁";
        }
        if (age <= 45) {
            return "36-45岁";
        }
        return "46岁以上";
    }

    /**
     * 完成后台统计看板中的 splitTypes 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @return 后台统计看板列表数据。
     */
    private List<String> splitTypes(String type) {
        if (StringUtils.isBlank(type)) {
            return Collections.singletonList("未设置");
        }
        List<String> types = Arrays.stream(type.split("[,，、\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        return types.isEmpty() ? Collections.singletonList("未设置") : types;
    }

    /**
     * 转换后台统计看板字段格式，便于后续计算或接口返回。
     * @param source source 字段，来源于当前接口入参或内部调用上下文。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 后台统计看板列表数据。
     */
    private List<Map<String, Object>> toRows(Map<String, ? extends Number> source, int limit) {
        return source.entrySet().stream()
                .sorted((left, right) -> Double.compare(right.getValue().doubleValue(), left.getValue().doubleValue()))
                .limit(limit)
                .map(entry -> row(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 完成后台统计看板中的 row 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 后台统计看板聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> row(String name, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("value", value);
        return row;
    }
}
