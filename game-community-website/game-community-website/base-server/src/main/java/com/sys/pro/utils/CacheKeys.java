package com.sys.pro.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Redis 缓存键工具，集中生成游戏、帖子、评价、榜单和支付相关缓存键。
 */
public final class CacheKeys {

    private static final String PREFIX = "gc:cache:";

    public static final String GAME_PATTERN = PREFIX + "game:*";
    public static final String POST_PATTERN = PREFIX + "post:*";
    public static final String RATING_PATTERN = PREFIX + "rating:*";
    public static final String NEWS_PATTERN = PREFIX + "news:*";
    public static final String RANKING_PATTERN = PREFIX + "ranking:*";
    public static final String MESSAGE_PATTERN = PREFIX + "message:*";
    public static final String COMMERCE_PATTERN = PREFIX + "commerce:*";
    public static final String PAYMENT_PATTERN = PREFIX + "payment:*";

    /**
     * 工具类只提供静态缓存键生成方法，不允许被实例化。
     */
    private CacheKeys() {
    }


    /**
     * 生成游戏全量列表缓存键，关键词会参与摘要避免不同搜索条件互相覆盖。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @return Redis 缓存键字符串。
     */
    public static String gameListAll(String keyword) {
        return PREFIX + "game:listAll:" + digest(keyword == null ? "" : keyword.trim());
    }

    /**
     * 生成游戏分页列表缓存键，使用查询参数摘要区分不同筛选结果。
     * @param payload 参与缓存键摘要计算的筛选参数串。
     * @return Redis 缓存键字符串。
     */
    public static String gameList(String payload) {
        return PREFIX + "game:list:" + digest(payload);
    }

    /**
     * 生成热门游戏 TOP10 缓存键，供首页侧栏复用。
     * @return Redis 缓存键字符串。
     */
    public static String gameHot() {
        return PREFIX + "game:hot:top10";
    }

    /**
     * 生成热门社区缓存键，按返回数量隔离不同榜单长度。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return Redis 缓存键字符串。
     */
    public static String gameHotCommunities(Integer limit) {
        return PREFIX + "game:hotCommunities:" + (limit == null ? 5 : limit);
    }

    /**
     * 生成游戏类型列表缓存键，减少类型筛选栏重复查询。
     * @return Redis 缓存键字符串。
     */
    public static String gameTypes() {
        return PREFIX + "game:types";
    }

    /**
     * 生成限时优惠游戏缓存键，供游戏中心优惠入口读取。
     * @return Redis 缓存键字符串。
     */
    public static String gameDiscounts() {
        return PREFIX + "game:discounts";
    }

    /**
     * 生成榜单模块缓存键，用筛选参数摘要区分不同榜单。
     * @param payload 参与缓存键摘要计算的筛选参数串。
     * @return Redis 缓存键字符串。
     */
    public static String ranking(String payload) {
        return PREFIX + "ranking:" + digest(payload);
    }

    /**
     * 生成游戏详情统计缓存键，缓存评分、销量和趋势数据。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String gameStats(Integer gameId) {
        return PREFIX + "game:stats:" + gameId;
    }

    /**
     * 生成游戏平均评分缓存键，避免详情页频繁聚合评价表。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String ratingAverage(Integer gameId) {
        return PREFIX + "rating:avg:" + gameId;
    }

    /**
     * 生成游戏评价数量缓存键，支撑评价类型和好评率展示。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String ratingCount(Integer gameId) {
        return PREFIX + "rating:count:" + gameId;
    }

    /**
     * 生成游戏评价分页缓存键，按页码、页大小和筛选条件隔离结果。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String ratingList(Integer gameId) {
        return PREFIX + "rating:list:" + gameId;
    }

    /**
     * 生成游戏评价分页缓存键，按页码、页大小和筛选条件隔离结果。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @param recommend 评价筛选标记，用来区分好评、差评或全部评价。
     * @return Redis 缓存键字符串。
     */
    public static String ratingList(Integer gameId, Integer pageNo, Integer pageSize, Boolean recommend) {
        return ratingList(gameId, pageNo, pageSize, recommend, null, null);
    }

    /**
     * 生成游戏评价分页缓存键，日期筛选会参与摘要计算，避免不同时间范围的评价列表互相命中缓存。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @param recommend 评价筛选标记，用来区分推荐、不推荐或全部评价。
     * @param startTime 评价发布时间起点，空值表示不限制开始时间。
     * @param endTime 评价发布时间终点，空值表示不限制结束时间。
     * @return Redis 缓存键字符串。
     */
    public static String ratingList(Integer gameId, Integer pageNo, Integer pageSize, Boolean recommend,
                                    String startTime, String endTime) {
        String payload = (gameId == null ? 0 : gameId)
                + ":" + (pageNo == null ? 1 : pageNo)
                + ":" + (pageSize == null ? 50 : pageSize)
                + ":" + (recommend == null ? "all" : recommend)
                + ":" + safe(startTime)
                + ":" + safe(endTime);
        return PREFIX + "rating:list:" + digest(payload);
    }

    /**
     * 生成社区帖子分页缓存键，按筛选条件摘要隔离不同帖子列表。
     * @param payload 参与缓存键摘要计算的筛选参数串。
     * @return Redis 缓存键字符串。
     */
    public static String postPage(String payload) {
        return PREFIX + "post:page:" + digest(payload);
    }

    /**
     * 生成热门话题缓存键，按数量限制区分展示范围。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return Redis 缓存键字符串。
     */
    public static String postHotTopics(Integer limit) {
        return PREFIX + "post:hotTopics:" + (limit == null ? 10 : limit);
    }

    /**
     * 生成指定游戏社区帖子缓存键，用于游戏标签下的帖子列表。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String postGame(Integer gameId) {
        return PREFIX + "post:game:" + gameId;
    }

    /**
     * 生成游戏社区帖子全量缓存键，供需要完整列表的场景读取。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String postGameAll(Long gameId) {
        return PREFIX + "post:gameAll:" + gameId;
    }

    /**
     * 生成私信会话缓存键，按会话双方隔离聊天记录。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param otherUserId otherUser 主键，用来定位关联业务数据。
     * @return Redis 缓存键字符串。
     */
    public static String messageConversation(Integer userId, Integer otherUserId) {
        int first = Math.min(userId == null ? 0 : userId, otherUserId == null ? 0 : otherUserId);
        int second = Math.max(userId == null ? 0 : userId, otherUserId == null ? 0 : otherUserId);
        return PREFIX + "message:conversation:" + first + ":" + second;
    }

    /**
     * 生成私信列表缓存键，缓存当前用户最近会话摘要。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return Redis 缓存键字符串。
     */
    public static String messageChatList(Integer userId) {
        return PREFIX + "message:chatList:" + (userId == null ? 0 : userId);
    }

    /**
     * 生成愿望单或购物车状态缓存键，按用户和游戏隔离按钮状态。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return Redis 缓存键字符串。
     */
    public static String commerceStatus(String type, Integer userId, Integer gameId) {
        return PREFIX + "commerce:status:" + safe(type) + ":" + (userId == null ? 0 : userId) + ":" + (gameId == null ? 0 : gameId);
    }

    /**
     * 生成愿望单或购物车列表缓存键，缓存当前用户的商品清单。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return Redis 缓存键字符串。
     */
    public static String commerceList(String type, Integer userId) {
        return PREFIX + "commerce:list:" + safe(type) + ":" + (userId == null ? 0 : userId);
    }

    /**
     * 生成已拥有游戏汇总缓存键，缓存数量、账号价值和游戏列表。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return Redis 缓存键字符串。
     */
    public static String ownedSummary(Integer userId) {
        return PREFIX + "commerce:owned:" + (userId == null ? 0 : userId);
    }

    /**
     * 获取订单扫码支付二维码和订单摘要信息。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return Redis 缓存键字符串。
     */
    public static String paymentQr(String orderNo) {
        return PREFIX + "payment:qr:" + safe(orderNo);
    }

    /**
     * 生成支付状态缓存键，降低支付轮询对数据库和支付宝接口的压力。
     * @param orderNo 订单号，用来查询支付、退款或取消状态。
     * @return Redis 缓存键字符串。
     */
    public static String paymentStatus(String orderNo) {
        return PREFIX + "payment:status:" + safe(orderNo);
    }

    /**
     * 把空文本转换成兜底值，避免拼接上下文出现 null。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存键字符串。
     */
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 完成CacheKeys中的 digest 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return Redis 缓存键字符串。
     */
    private static String digest(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] bytes = messageDigest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return String.valueOf((value == null ? "" : value).hashCode());
        }
    }
}
