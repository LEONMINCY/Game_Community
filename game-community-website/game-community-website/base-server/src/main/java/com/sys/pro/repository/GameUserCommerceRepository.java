package com.sys.pro.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户游戏关系持久化仓储。
 * 原始 SQL 只保留在 Repository 层，避免 Controller/Service 直接拼接数据库访问逻辑。
 */
@Repository
@RequiredArgsConstructor
public class GameUserCommerceRepository {

    public static final String CART_TABLE = "t_game_cart";
    public static final String WISHLIST_TABLE = "t_game_wishlist";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 判断愿望单、购物车和已购游戏当前状态是否满足业务条件。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    public boolean isActive(String tableName, Integer userId, Integer gameId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + relationTable(tableName) + " WHERE user_id = ? AND game_id = ? AND deleted = 0",
                Integer.class,
                userId,
                gameId
        );
        return count != null && count > 0;
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 exists 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return true 表示愿望单、购物车和已购游戏当前状态满足业务判断。
     */
    public boolean exists(String tableName, Integer userId, Integer gameId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + relationTable(tableName) + " WHERE user_id = ? AND game_id = ?",
                Integer.class,
                userId,
                gameId
        );
        return count != null && count > 0;
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 updateDeleted 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param deleted deleted 字段，来源于当前接口入参或内部调用上下文。
     */
    public void updateDeleted(String tableName, Integer userId, Integer gameId, boolean deleted) {
        jdbcTemplate.update("UPDATE " + relationTable(tableName) + " SET deleted = ? WHERE user_id = ? AND game_id = ?",
                deleted ? 1 : 0, userId, gameId);
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 insertRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     */
    public void insertRelation(String tableName, Integer userId, Integer gameId) {
        jdbcTemplate.update("INSERT INTO " + relationTable(tableName) + " (user_id, game_id, create_time, deleted) VALUES (?, ?, NOW(), 0)",
                userId, gameId);
    }

    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    public List<Map<String, Object>> listRelationGames(String tableName, Integer userId) {
        return jdbcTemplate.queryForList(
                "SELECT r.id AS relationId, g.id, g.id AS gameId, g.name, g.icon, g.developer, g.price, " +
                        "COALESCE(g.discount, 0) AS discount, r.create_time AS createTime, " +
                        "EXISTS(SELECT 1 FROM t_order o WHERE o.user_id = r.user_id AND o.game_id = g.id " +
                        "AND o.status = '待支付' AND (o.deleted = 0 OR o.deleted IS NULL)) AS pendingOrder " +
                        "FROM " + relationTable(tableName) + " r JOIN t_game g ON r.game_id = g.id " +
                        "WHERE r.user_id = ? AND r.deleted = 0 AND (g.deleted = 0 OR g.deleted IS NULL) " +
                        "ORDER BY r.create_time DESC",
                userId
        );
    }

    /**
     * 汇总愿望单、购物车和已购游戏列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    public List<Map<String, Object>> listOwnedGames(Integer userId) {
        return jdbcTemplate.queryForList(
                "SELECT g.id, g.id AS gameId, g.name, g.icon, g.developer, g.price, " +
                        "COALESCE(g.discount, 0) AS discount, g.price_mark AS priceMark, MIN(o.create_time) AS purchaseTime " +
                        "FROM t_order o JOIN t_game g ON o.game_id = g.id " +
                        "WHERE o.user_id = ? AND o.status = '购买成功' AND (o.deleted = 0 OR o.deleted IS NULL) " +
                        "AND (g.deleted = 0 OR g.deleted IS NULL) " +
                        "GROUP BY g.id, g.name, g.icon, g.developer, g.price, g.discount, g.price_mark " +
                        "ORDER BY purchaseTime DESC",
                userId
        );
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 findActiveCartGameIds 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     * @return 愿望单、购物车和已购游戏列表数据。
     */
    public List<Integer> findActiveCartGameIds(Integer userId, List<Integer> gameIds) {
        if (CollectionUtils.isEmpty(gameIds)) {
            return new ArrayList<>();
        }
        String placeholders = gameIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(gameIds);
        return jdbcTemplate.queryForList(
                "SELECT game_id FROM t_game_cart WHERE user_id = ? AND deleted = 0 AND game_id IN (" + placeholders + ")",
                Integer.class,
                params.toArray()
        );
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 clearCartItems 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     */
    public void clearCartItems(Integer userId, List<Integer> gameIds) {
        if (CollectionUtils.isEmpty(gameIds)) {
            return;
        }
        String placeholders = gameIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(gameIds);
        jdbcTemplate.update("UPDATE t_game_cart SET deleted = 1 WHERE user_id = ? AND game_id IN (" + placeholders + ")", params.toArray());
    }

    /**
     * 完成愿望单、购物车和已购游戏中的 relationTable 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @return 愿望单、购物车和已购游戏处理后的文本结果。
     */
    private String relationTable(String tableName) {
        if (CART_TABLE.equals(tableName) || WISHLIST_TABLE.equals(tableName)) {
            return tableName;
        }
        /**
         * 完成愿望单、购物车和已购游戏中的 IllegalArgumentException 步骤，保证该环节的数据和状态可以继续向下流转。
         * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
         * @return 愿望单、购物车和已购游戏在该步骤产出的业务结果。
         */
        throw new IllegalArgumentException("Unsupported game relation table: " + tableName);
    }
}
