package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
/**
 * 数据库性能初始化组件，启动时补齐常用查询索引。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabasePerformanceInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.performance.auto-index:true}")
    private boolean autoIndex;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        if (!autoIndex) {
            return;
        }
        performanceIndexes().forEach(this::ensureIndexSafely);
    }

    /**
     * 列出项目高频查询需要的索引定义，供启动初始化逐项检查。
     * @return 数据库结构列表数据。
     */
    private List<IndexSpec> performanceIndexes() {
        return Arrays.asList(
                // 订单列表、支付轮询和超时取消都是高频路径，按筛选字段 + 时间排序建立复合索引。
                new IndexSpec("t_order", "idx_order_order_no", "order_no"),
                new IndexSpec("t_order", "idx_order_user_status_time", "user_id", "status", "create_time"),
                new IndexSpec("t_order", "idx_order_user_game_status", "user_id", "game_id", "status"),
                new IndexSpec("t_order", "idx_order_game_status_time", "game_id", "status", "create_time"),
                new IndexSpec("t_order", "idx_order_refund_status_time", "refund_status", "create_time"),
                new IndexSpec("t_order", "idx_order_user_deleted_time", "user_id", "deleted", "create_time"),
                new IndexSpec("t_order", "idx_order_deleted_time", "deleted", "create_time"),
                new IndexSpec("t_order", "idx_order_status_time", "status", "create_time"),

                // 游戏中心按名称、类型、开发商和折扣筛选，前缀列保持与查询条件一致。
                new IndexSpec("t_game", "idx_game_deleted_name", "deleted", "name"),
                new IndexSpec("t_game", "idx_game_deleted_type", "deleted", "type"),
                new IndexSpec("t_game", "idx_game_deleted_developer", "deleted", "developer"),
                new IndexSpec("t_game", "idx_game_discount_deleted", "discount", "deleted"),

                // 社区列表、个人动态、审核列表都按审核状态/用户/游戏筛选并按发布时间倒序展示。
                new IndexSpec("t_post", "idx_post_deleted_audit_game_time", "deleted", "audit_status", "game_id", "create_time"),
                new IndexSpec("t_post", "idx_post_deleted_game_time", "deleted", "game_id", "create_time"),
                new IndexSpec("t_post", "idx_post_user_deleted_time", "user_id", "deleted", "create_time"),
                new IndexSpec("t_post", "idx_post_audit_appeal_time", "audit_status", "appeal_status", "create_time"),
                new IndexSpec("t_post", "idx_post_title", "title"),

                // 评论区需要快速加载主评论、楼中楼回复，并统计子回复数量。
                new IndexSpec("t_comment", "idx_comment_post_deleted_time", "post_id", "deleted", "create_time"),
                new IndexSpec("t_comment", "idx_comment_post_parent_deleted_time", "post_id", "parent_id", "deleted", "create_time"),
                new IndexSpec("t_comment", "idx_comment_parent_deleted_time", "parent_id", "deleted", "create_time"),
                new IndexSpec("t_comment", "idx_comment_user_time", "user_id", "create_time"),

                new IndexSpec("t_game_rating", "idx_rating_game_time", "game_id", "create_time"),
                new IndexSpec("t_game_rating", "idx_rating_game_recommend_time", "game_id", "recommend", "create_time"),
                new IndexSpec("t_game_rating", "idx_rating_user_game", "user_id", "game_id"),

                new IndexSpec("t_news", "idx_news_deleted_time", "deleted", "create_time"),
                new IndexSpec("t_news", "idx_news_title", "title"),

                new IndexSpec("t_message", "idx_message_sender_receiver_time", "sender_id", "receiver_id", "create_time"),
                new IndexSpec("t_message", "idx_message_receiver_time", "receiver_id", "create_time"),

                new IndexSpec("t_user_relation", "idx_relation_user_deleted", "user_id", "deleted"),
                new IndexSpec("t_user_relation", "idx_relation_follow_deleted", "follow_id", "deleted"),

                new IndexSpec("t_player_user", "idx_player_user_created", "created_time"),
                new IndexSpec("t_player_user", "idx_player_user_last_login", "last_login"),
                new IndexSpec("t_player_user", "idx_player_user_deleted_username", "deleted", "username"),
                new IndexSpec("t_moderator_user", "idx_moderator_user_created", "created_time"),
                new IndexSpec("t_moderator_user", "idx_moderator_user_last_login", "last_login"),
                new IndexSpec("t_moderator_user", "idx_moderator_user_deleted_username", "deleted", "username"),
                new IndexSpec("t_admin_user", "idx_admin_user_created", "created_time"),
                new IndexSpec("t_admin_user", "idx_admin_user_last_login", "last_login"),
                new IndexSpec("t_admin_user", "idx_admin_user_deleted_username", "deleted", "username"),

                new IndexSpec("t_browse_history", "idx_browse_user_type_time", "user_id", "target_type", "browse_time"),
                new IndexSpec("t_browse_history", "idx_browse_target_type_time", "target_id", "target_type", "browse_time"),

                new IndexSpec("t_game_cart", "idx_cart_user_game_deleted", "user_id", "game_id", "deleted"),
                new IndexSpec("t_game_wishlist", "idx_wishlist_user_game_deleted", "user_id", "game_id", "deleted"),

                // 举报管理默认按创建时间倒序，也会按状态、类型和被举报人筛选。
                new IndexSpec("t_report", "idx_report_status_type_time", "status", "report_type", "create_time"),
                new IndexSpec("t_report", "idx_report_reported_type", "reported_id", "report_type"),
                new IndexSpec("t_report", "idx_report_deleted_time", "deleted", "create_time"),
                new IndexSpec("t_report", "idx_report_deleted_status_type_time", "deleted", "status", "report_type", "create_time"),

                // 用户主页和所有用户信息 JOIN 都依赖 user_id，必须避免扫描整张 t_user_info。
                new IndexSpec("t_user_info", "idx_user_info_user_id", "user_id"),
                new IndexSpec("t_user_info", "idx_user_info_deleted_nickname", "deleted", "nickname"),

                // 操作日志查询和导出按时间、用户、角色、结果筛选，索引保持后台筛选响应稳定。
                new IndexSpec("t_operation_log", "idx_operation_log_deleted_time", "deleted", "created_time"),
                new IndexSpec("t_operation_log", "idx_operation_log_username_time", "username", "created_time"),
                new IndexSpec("t_operation_log", "idx_operation_log_role_time", "role_id", "created_time"),
                new IndexSpec("t_operation_log", "idx_operation_log_success_time", "success", "created_time"),

                // 新闻评论详情页按新闻/父评论分页，并按时间排序。
                new IndexSpec("t_news_comment", "idx_news_comment_news_parent_time", "news_id", "deleted", "parent_id", "create_time"),
                new IndexSpec("t_news_comment", "idx_news_comment_parent_time", "parent_id", "deleted", "create_time")
        );
    }

    /**
     * 确保数据库结构依赖的数据或结构存在，避免运行时缺少基础配置。
     * @param spec spec 字段，来源于当前接口入参或内部调用上下文。
     */
    private void ensureIndexSafely(IndexSpec spec) {
        try {
            if (!tableExists(spec.table) || !columnsExist(spec.table, spec.columns) || indexExists(spec.table, spec.indexName)) {
                return;
            }
            String ddl = "CREATE INDEX `" + spec.indexName + "` ON `" + spec.table + "` (" +
                    spec.columns.stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) +
                    ")";
            jdbcTemplate.execute(ddl);
            log.info("Created mysql performance index {}.{}", spec.table, spec.indexName);
        } catch (Exception e) {
            log.warn("Skip mysql performance index {}.{}", spec.table, spec.indexName, e);
        }
    }

    /**
     * 检查当前数据库是否存在目标表，避免重复执行 DDL。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示数据库结构当前状态满足业务判断。
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    /**
     * 检查目标索引是否已经存在，避免重复创建索引。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param indexName indexName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示数据库结构当前状态满足业务判断。
     */
    private boolean indexExists(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    /**
     * 确认索引依赖的字段都已存在，再执行索引创建。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param columns columns 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示数据库结构当前状态满足业务判断。
     */
    private boolean columnsExist(String tableName, List<String> columns) {
        String placeholders = columns.stream().map(column -> "?").collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(tableName);
        params.addAll(columns);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME IN (" + placeholders + ")",
                Integer.class,
                params.toArray()
        );
        return count != null && count == columns.size();
    }

    private static class IndexSpec {
        private final String table;
        private final String indexName;
        private final List<String> columns;

        /**
         * 描述一个需要自动创建的 MySQL 索引。
         *
         * @param table 目标表名。
         * @param indexName 索引名称。
         * @param columns 参与索引的字段列表，顺序会直接影响组合索引效果。
         */
        private IndexSpec(String table, String indexName, String... columns) {
            this.table = table;
            this.indexName = indexName;
            this.columns = Arrays.asList(columns);
        }
    }
}
