package com.sys.pro.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sys.pro.mapper.CommentMapper;
import com.sys.pro.pojo.Comment;
import com.sys.pro.pojo.Game;
import com.sys.pro.pojo.Order;
import com.sys.pro.pojo.Post;
import com.sys.pro.pojo.UserInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 站内上下文服务，负责把社区里的游戏、帖子、评论和订单数据整理成模型可读的摘要。
 */
@Service
@RequiredArgsConstructor
public class AiCommunityContextService {

    private static final int MAX_SITE_CONTEXT_LENGTH = 12000;
    private static final int MAX_POST_CONTEXT_LENGTH = 7000;
    private static final String ORDER_SUCCESS = "购买成功";
    private static final String ORDER_SUCCESS_MOJIBAKE = "璐拱鎴愬姛";

    private final GameService gameService;
    private final PostService postService;
    private final CommentMapper commentMapper;
    private final GameRatingService gameRatingService;
    private final OrderService orderService;
    private final UserInfoService userInfoService;

    /**
     * 组装社区管理所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @return 社区管理处理后的文本结果。
     */
    public String buildContext() {
        List<Post> posts = activePosts();
        List<Game> games = activeGames();

        StringBuilder builder = new StringBuilder();
        builder.append("以下是当前游戏社区网站的站内数据摘要。回答本站相关问题时，请优先依据这些数据，无法确定时明确说明。")
                .append("\n站内概览：游戏总数 ").append(games.size())
                .append("，已通过审核帖子数 ").append(posts.size())
                .append("，帖子评论总数 ").append(activeCommentCount())
                .append("，成功订单数 ").append(successfulOrders().size())
                .append("。");

        appendGames(builder, games, posts);
        appendDiscounts(builder, games);
        appendLatestPosts(builder, posts);
        appendHotPosts(builder, posts);

        return trimToLength(builder.toString(), MAX_SITE_CONTEXT_LENGTH);
    }

    /**
     * 组装社区管理所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param postId 帖子主键，用来定位评论、点赞或审核所属的帖子。
     * @return 社区管理处理后的文本结果。
     */
    public String buildPostContext(Integer postId) {
        if (postId == null) {
            return "";
        }
        Post post = postService.getById(postId);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return "\n\n[当前帖子上下文]\n当前评论关联的帖子不存在或已删除。";
        }

        StringBuilder builder = new StringBuilder("\n\n[当前帖子上下文]");
        builder.append("\n帖子标题：").append(safe(post.getTitle()))
                .append("\n帖子作者：").append(authorName(post.getUserId()))
                .append("\n关联游戏：").append(gameName(post.getGameId()))
                .append("\n话题：").append(StringUtils.defaultIfBlank(post.getTopics(), "无"))
                .append("\n点赞数：").append(countCsv(post.getLikesUser()))
                .append("，收藏数：").append(countCsv(post.getFavoritesUser()))
                .append("，转发数：").append(post.getShareCount() == null ? 0 : post.getShareCount())
                .append("\n帖子正文：").append(shortText(post.getContent(), 1800));

        appendPostGameDetail(builder, post.getGameId());
        appendPostComments(builder, postId);
        return trimToLength(builder.toString(), MAX_POST_CONTEXT_LENGTH);
    }

    /**
     * 筛选有效游戏数据，排除已删除或不可展示的记录。
     * @return 社区管理列表数据。
     */
    private List<Game> activeGames() {
        return gameService.list().stream()
                .filter(game -> !Boolean.TRUE.equals(game.getDeleted()))
                .collect(Collectors.toList());
    }

    /**
     * 筛选可展示帖子数据，排除未审核或已删除内容。
     * @return 社区管理列表数据。
     */
    private List<Post> activePosts() {
        return postService.list().stream()
                .filter(post -> !Boolean.TRUE.equals(post.getDeleted()))
                .filter(this::approvedPost)
                .collect(Collectors.toList());
    }

    /**
     * 筛选支付成功订单，供 AI 和统计模块读取销量信息。
     * @return 社区管理列表数据。
     */
    private List<Order> successfulOrders() {
        return orderService.lambdaQuery()
                .in(Order::getStatus, ORDER_SUCCESS, ORDER_SUCCESS_MOJIBAKE)
                .list();
    }

    /**
     * 判断帖子是否已审核通过，可进入 AI 社区上下文。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示社区管理当前状态满足业务判断。
     */
    private boolean approvedPost(Post post) {
        return post != null && (StringUtils.isBlank(post.getAuditStatus()) || "approved".equals(post.getAuditStatus()));
    }

    /**
     * 把游戏基础资料写入 AI 上下文，帮助模型回答站内游戏问题。
     * @param builder builder 字段，来源于当前接口入参或内部调用上下文。
     * @param games games 字段，来源于当前接口入参或内部调用上下文。
     * @param posts posts 字段，来源于当前接口入参或内部调用上下文。
     */
    private void appendGames(StringBuilder builder, List<Game> games, List<Post> posts) {
        Map<Integer, Long> postCountMap = posts.stream()
                .filter(post -> post.getGameId() != null)
                .collect(Collectors.groupingBy(Post::getGameId, Collectors.counting()));
        Map<Integer, Long> salesCountMap = successfulOrders().stream()
                .filter(order -> order.getGameId() != null)
                .collect(Collectors.groupingBy(Order::getGameId, Collectors.counting()));

        builder.append("\n\n热门游戏TOP10（综合销量、评分、发帖量）：");
        games.stream()
                .sorted((left, right) -> Double.compare(
                        gameHotScore(right, postCountMap, salesCountMap),
                        gameHotScore(left, postCountMap, salesCountMap)))
                .limit(10)
                .forEach(game -> builder.append("\n- ")
                        .append(safe(game.getName()))
                        .append("；类型：").append(safe(game.getType()))
                        .append("；平台：").append(safe(game.getPlatforms()))
                        .append("；开发商：").append(safe(game.getDeveloper()))
                        .append("；评分：").append(formatRating(game.getId()))
                        .append("；销量：").append(salesCountMap.getOrDefault(game.getId(), 0L))
                        .append("；帖子数：").append(postCountMap.getOrDefault(game.getId(), 0L))
                        .append("；价格：").append(formatPrice(game)));
    }

    /**
     * 把当前优惠游戏写入 AI 上下文，支持优惠推荐问答。
     * @param builder builder 字段，来源于当前接口入参或内部调用上下文。
     * @param games games 字段，来源于当前接口入参或内部调用上下文。
     */
    private void appendDiscounts(StringBuilder builder, List<Game> games) {
        List<Game> discounts = games.stream()
                .filter(game -> game.getDiscount() != null && game.getDiscount() > 0)
                .sorted(Comparator.comparing(Game::getDiscount).reversed())
                .limit(10)
                .collect(Collectors.toList());
        if (discounts.isEmpty()) {
            return;
        }
        builder.append("\n\n正在限时优惠的游戏：");
        discounts.forEach(game -> builder.append("\n- ")
                .append(safe(game.getName()))
                .append("：原价 ").append(game.getPrice() == null ? 0 : game.getPrice())
                .append("，折扣 ").append(game.getDiscount()).append("%")
                .append("，折后价 ").append(finalPrice(game))
                .append(StringUtils.isBlank(game.getPriceMark()) ? "" : "，价格标记：" + game.getPriceMark()));
    }

    /**
     * 追加最新帖子摘要，让 AI 能了解社区正在讨论的内容。
     * @param builder builder 字段，来源于当前接口入参或内部调用上下文。
     * @param posts posts 字段，来源于当前接口入参或内部调用上下文。
     */
    private void appendLatestPosts(StringBuilder builder, List<Post> posts) {
        builder.append("\n\n最新通过审核帖子：");
        posts.stream()
                .sorted(Comparator.comparing(Post::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(12)
                .forEach(post -> builder.append("\n- 《")
                        .append(safe(post.getTitle()))
                        .append("》；关联游戏：").append(gameName(post.getGameId()))
                        .append("；作者：").append(authorName(post.getUserId()))
                        .append("；点赞 ").append(countCsv(post.getLikesUser()))
                        .append("，收藏 ").append(countCsv(post.getFavoritesUser()))
                        .append("；摘要：").append(shortText(post.getContent(), 120)));
    }

    /**
     * 追加热门帖子摘要，让 AI 能引用高热讨论。
     * @param builder builder 字段，来源于当前接口入参或内部调用上下文。
     * @param posts posts 字段，来源于当前接口入参或内部调用上下文。
     */
    private void appendHotPosts(StringBuilder builder, List<Post> posts) {
        builder.append("\n\n互动热帖TOP10（点赞+收藏+评论）：");
        posts.stream()
                .sorted((left, right) -> Integer.compare(postHeat(right), postHeat(left)))
                .limit(10)
                .forEach(post -> builder.append("\n- 《")
                        .append(safe(post.getTitle()))
                        .append("》；关联游戏：").append(gameName(post.getGameId()))
                        .append("；互动分：").append(postHeat(post))
                        .append("；摘要：").append(shortText(post.getContent(), 120)));
    }

    /**
     * 把帖子关联游戏详情补进 AI 回答上下文。
     * @param builder builder 字段，来源于当前接口入参或内部调用上下文。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    private void appendPostGameDetail(StringBuilder builder, Integer gameId) {
        if (gameId == null) {
            return;
        }
        Game game = gameService.getById(gameId);
        if (game == null || Boolean.TRUE.equals(game.getDeleted())) {
            return;
        }
        builder.append("\n关联游戏详情：")
                .append(safe(game.getName()))
                .append("；类型：").append(safe(game.getType()))
                .append("；平台：").append(safe(game.getPlatforms()))
                .append("；开发商：").append(safe(game.getDeveloper()))
                .append("；发售日期：").append(game.getReleaseDate() == null ? "未设置" : game.getReleaseDate())
                .append("；评分：").append(formatRating(gameId))
                .append("；简介：").append(shortText(game.getDescription(), 300));
    }

    /**
     * 把帖子评论摘要补进 AI 回答上下文。
     * @param builder builder 字段，来源于当前接口入参或内部调用上下文。
     * @param postId 帖子主键，用来定位评论、点赞或审核所属的帖子。
     */
    private void appendPostComments(StringBuilder builder, Integer postId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getDeleted, false)
                .orderByDesc(Comment::getCreateTime)
                .last("LIMIT 20"));
        if (comments == null || comments.isEmpty()) {
            builder.append("\n该帖子暂无评论。");
            return;
        }

        List<Comment> ordered = new ArrayList<>(comments);
        ordered.sort(Comparator.comparing(Comment::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())));
        builder.append("\n最近评论片段：");
        ordered.forEach(comment -> builder.append("\n- ")
                .append(authorName(comment.getUserId()))
                .append(comment.getReplyUserId() == null ? "" : " 回复 " + authorName(comment.getReplyUserId()))
                .append("：")
                .append(shortText(comment.getContent(), 160)));
    }

    /**
     * 统计帖子有效评论数量，衡量讨论热度。
     * @return 社区管理统计值或主键结果。
     */
    private long activeCommentCount() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getDeleted, false));
    }

    /**
     * 综合销量、评价和帖子量计算游戏热度分。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @param postCountMap postCountMap 字段，来源于当前接口入参或内部调用上下文。
     * @param salesCountMap salesCountMap 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理在该步骤产出的业务结果。
     */
    private double gameHotScore(Game game, Map<Integer, Long> postCountMap, Map<Integer, Long> salesCountMap) {
        if (game == null || game.getId() == null) {
            return 0;
        }
        return salesCountMap.getOrDefault(game.getId(), 0L) * 3
                + safeRating(game.getId()) * 2
                + postCountMap.getOrDefault(game.getId(), 0L);
    }

    /**
     * 格式化评分文本，避免空评分影响 AI 输出。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 社区管理处理后的文本结果。
     */
    private String formatRating(Integer gameId) {
        if (gameId == null) {
            return "暂无";
        }
        return BigDecimal.valueOf(safeRating(gameId))
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString();
    }

    /**
     * 把评分字段安全转换为数值。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 社区管理在该步骤产出的业务结果。
     */
    private double safeRating(Integer gameId) {
        try {
            return gameRatingService.getGameAverageRating(gameId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 格式化游戏价格，兼容免费和打折显示。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理处理后的文本结果。
     */
    private String formatPrice(Game game) {
        if (game.getPrice() == null) {
            return "未设置";
        }
        if (game.getDiscount() != null && game.getDiscount() > 0) {
            return game.getPrice() + "，折后 " + finalPrice(game) + "，" + game.getDiscount() + "%";
        }
        return String.valueOf(game.getPrice());
    }

    /**
     * 根据原价和折扣计算实际支付价格。
     * @param game game 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理统计值或主键结果。
     */
    private int finalPrice(Game game) {
        int price = game.getPrice() == null ? 0 : game.getPrice();
        int discount = game.getDiscount() == null ? 0 : Math.max(0, Math.min(game.getDiscount(), 100));
        if (discount <= 0) {
            return price;
        }
        return BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(100 - discount))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 根据点赞、收藏、评论和转发计算帖子热度。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理统计值或主键结果。
     */
    private int postHeat(Post post) {
        long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, post.getId())
                .eq(Comment::getDeleted, false));
        return countCsv(post.getLikesUser()) + countCsv(post.getFavoritesUser()) + (int) commentCount;
    }

    /**
     * 根据游戏主键读取游戏名称，缺失时返回兜底文本。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 社区管理处理后的文本结果。
     */
    private String gameName(Integer gameId) {
        if (gameId == null) {
            return "未关联游戏";
        }
        Game game = gameService.getById(gameId);
        return game == null ? "未知游戏" : safe(game.getName());
    }

    /**
     * 根据用户主键读取作者昵称，缺失时返回匿名展示名。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 社区管理处理后的文本结果。
     */
    private String authorName(Integer userId) {
        if (userId == null) {
            return "未知用户";
        }
        if (userId <= 0) {
            return "AI助手";
        }
        UserInfo userInfo = userInfoService.getById(userId);
        return userInfo == null ? "未知用户" : safe(userInfo.getNickname());
    }

    /**
     * 统计逗号分隔字段中的有效元素数量。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理统计值或主键结果。
     */
    private int countCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        return (int) Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .count();
    }

    /**
     * 压缩富文本内容，提取适合 AI 上下文的短摘要。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param maxLength maxLength 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理处理后的文本结果。
     */
    private String shortText(String value, int maxLength) {
        String text = safe(value)
                .replaceAll("(?s)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 按最大长度裁剪文本，防止 AI 上下文过长。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param maxLength maxLength 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理处理后的文本结果。
     */
    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n[站内摘要已截断，仅保留最相关的前半部分]";
    }

    /**
     * 把空文本转换成兜底值，避免拼接上下文出现 null。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理处理后的文本结果。
     */
    private String safe(String value) {
        return StringUtils.defaultIfBlank(value, "未设置");
    }
}
