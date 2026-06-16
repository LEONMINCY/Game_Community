package com.sys.pro.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 榜单查询仓储。
 * 所有榜单相关 SQL 统一放在这里，Service 只做缓存、排序装饰和图表数据聚合。
 */
@Repository
@RequiredArgsConstructor
public class RankingRepository {

    private static final String PAID_STATUS = "\u8d2d\u4e70\u6210\u529f";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 完成游戏榜单中的 selectPlatformValues 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 游戏榜单列表数据。
     */
    public List<String> selectPlatformValues() {
        return jdbcTemplate.queryForList(
                "SELECT platforms FROM t_game WHERE COALESCE(deleted, 0) = 0 AND platforms IS NOT NULL AND platforms <> ''",
                String.class);
    }

    /**
     * 完成游戏榜单中的 queryRevenueRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @param discountsOnly discountsOnly 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    public List<Map<String, Object>> queryRevenueRanking(LocalDateTime start,
                                                         LocalDateTime end,
                                                         String type,
                                                         String platform,
                                                         boolean discountsOnly) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT g.id, g.name, g.icon, g.type, g.platforms, g.price, g.discount, g.price_mark, ");
        sql.append("COALESCE(SUM(o.total_price), 0) revenue, COUNT(o.id) sales ");
        sql.append("FROM t_game g LEFT JOIN t_order o ON o.game_id = g.id ");
        appendPaidOrderJoin(sql, params, start, end);
        sql.append("WHERE COALESCE(g.deleted, 0) = 0 ");
        if (discountsOnly) {
            sql.append("AND COALESCE(g.discount, 0) > 0 ");
        }
        appendGameFilters(sql, params, type, platform);
        sql.append("GROUP BY g.id, g.name, g.icon, g.type, g.platforms, g.price, g.discount, g.price_mark ");
        sql.append("ORDER BY revenue DESC, sales DESC, g.id ASC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 完成游戏榜单中的 queryReviewRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    public List<Map<String, Object>> queryReviewRanking(String type, String platform) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT g.id, g.name, g.icon, g.type, g.platforms, g.price, g.discount, ");
        sql.append("COUNT(r.id) review_count, ");
        sql.append("SUM(CASE WHEN r.recommend IS NULL OR r.recommend = 1 THEN 1 ELSE 0 END) good_count, ");
        sql.append("SUM(CASE WHEN r.recommend = 0 THEN 1 ELSE 0 END) bad_count, ");
        sql.append("AVG(r.rating) avg_rating ");
        sql.append("FROM t_game g LEFT JOIN t_game_rating r ON r.game_id = g.id ");
        sql.append("WHERE COALESCE(g.deleted, 0) = 0 ");
        appendGameFilters(sql, params, type, platform);
        sql.append("GROUP BY g.id, g.name, g.icon, g.type, g.platforms, g.price, g.discount ");
        sql.append("ORDER BY CASE WHEN COUNT(r.id) = 0 THEN 0 ELSE SUM(CASE WHEN r.recommend IS NULL OR r.recommend = 1 THEN 1 ELSE 0 END) / COUNT(r.id) END DESC, review_count DESC, avg_rating DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 完成游戏榜单中的 queryExpectedRanking 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     * @return 游戏榜单列表数据。
     */
    public List<Map<String, Object>> queryExpectedRanking(String type, String platform) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT g.id, g.name, g.icon, g.type, g.platforms, g.price, g.discount, COUNT(w.id) wishlist_count ");
        sql.append("FROM t_game g LEFT JOIN t_game_wishlist w ON w.game_id = g.id AND COALESCE(w.deleted, 0) = 0 ");
        sql.append("WHERE COALESCE(g.deleted, 0) = 0 ");
        appendGameFilters(sql, params, type, platform);
        sql.append("GROUP BY g.id, g.name, g.icon, g.type, g.platforms, g.price, g.discount ");
        sql.append("ORDER BY wishlist_count DESC, g.id ASC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 完成游戏榜单中的 queryReviewCounts 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏榜单聚合数据，键名与前端展示字段保持一致。
     */
    public Map<String, Object> queryReviewCounts(Integer gameId) {
        return jdbcTemplate.queryForMap(
                "SELECT COUNT(*) total, SUM(CASE WHEN recommend IS NULL OR recommend = 1 THEN 1 ELSE 0 END) good_count, SUM(CASE WHEN recommend = 0 THEN 1 ELSE 0 END) bad_count FROM t_game_rating WHERE game_id = ?",
                gameId);
    }

    /**
     * 完成游戏榜单中的 queryReviewTrend 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @return 游戏榜单列表数据。
     */
    public List<Map<String, Object>> queryReviewTrend(Integer gameId) {
        return jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(create_time, '%Y-%m') month, COUNT(*) total, SUM(CASE WHEN recommend IS NULL OR recommend = 1 THEN 1 ELSE 0 END) good_count FROM t_game_rating WHERE game_id = ? AND create_time IS NOT NULL GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month ASC",
                gameId);
    }

    /**
     * 完成游戏榜单中的 appendPaidOrderJoin 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param sql sql 字段，来源于当前接口入参或内部调用上下文。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     */
    private void appendPaidOrderJoin(StringBuilder sql, List<Object> params, LocalDateTime start, LocalDateTime end) {
        sql.append("AND (o.status = ? OR o.status = 'PAID' OR o.status = 'SUCCESS') ");
        params.add(PAID_STATUS);
        sql.append("AND COALESCE(o.deleted, 0) = 0 ");
        if (start != null) {
            sql.append("AND o.create_time >= ? ");
            params.add(Timestamp.valueOf(start));
        }
        if (end != null) {
            sql.append("AND o.create_time < ? ");
            params.add(Timestamp.valueOf(end));
        }
    }

    /**
     * 完成游戏榜单中的 appendGameFilters 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param sql sql 字段，来源于当前接口入参或内部调用上下文。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param platform platform 字段，来源于当前接口入参或内部调用上下文。
     */
    private void appendGameFilters(StringBuilder sql, List<Object> params, String type, String platform) {
        if (StringUtils.hasText(type)) {
            sql.append("AND g.type LIKE ? ");
            params.add("%" + type.trim() + "%");
        }
        if (StringUtils.hasText(platform)) {
            sql.append("AND g.platforms LIKE ? ");
            params.add("%" + platform.trim() + "%");
        }
    }
}
