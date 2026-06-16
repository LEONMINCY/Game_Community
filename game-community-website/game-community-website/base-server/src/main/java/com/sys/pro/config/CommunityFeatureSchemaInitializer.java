package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 社区功能表结构初始化组件，补齐举报、通知、话题等社区扩展字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityFeatureSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        addColumnIfAbsent("t_comment", "image_url", "varchar(500) NULL COMMENT '评论图片'");
        addColumnIfAbsent("t_comment", "likes_user", "text NULL COMMENT '评论点赞用户ID集合'");
        addColumnIfAbsent("t_report", "reply", "varchar(500) NULL COMMENT '举报处理回复'");
        addColumnIfAbsent("t_report", "update_time", "datetime NULL COMMENT '处理时间'");
        addColumnIfAbsent("t_report", "evidence_text", "text NULL COMMENT '举报文字证据'");
        addColumnIfAbsent("t_report", "evidence_images", "text NULL COMMENT '举报图片证据'");
        addColumnIfAbsent("t_report", "appeal_content", "text NULL COMMENT '被举报人申诉内容'");
        addColumnIfAbsent("t_report", "appeal_status", "varchar(32) NULL COMMENT '被举报人申诉状态'");
        addColumnIfAbsent("t_report", "appeal_reply", "varchar(500) NULL COMMENT '举报申诉处理回复'");
        addColumnIfAbsent("t_report", "appeal_time", "datetime NULL COMMENT '被举报人申诉时间'");
        addColumnIfAbsent("t_report", "appeal_review_time", "datetime NULL COMMENT '举报申诉审核时间'");
        addAccountColumnIfAbsent("ban_end_time", "datetime NULL COMMENT '封禁截止时间'");
        addColumnIfAbsent("t_user_info", "privacy_profile", "tinyint(1) NULL DEFAULT 0 COMMENT '隐藏个人资料'");
        addColumnIfAbsent("t_user_info", "privacy_follow", "tinyint(1) NULL DEFAULT 0 COMMENT '隐藏关注列表'");
        addColumnIfAbsent("t_user_info", "privacy_fans", "tinyint(1) NULL DEFAULT 0 COMMENT '隐藏粉丝列表'");
        addColumnIfAbsent("t_user_info", "email", "varchar(120) NULL COMMENT '绑定邮箱'");
        addColumnIfAbsent("t_news", "likes_user", "text NULL COMMENT '新闻点赞用户'");
        addColumnIfAbsent("t_news", "favorites_user", "text NULL COMMENT '新闻收藏用户'");
        addColumnIfAbsent("t_news", "share_count", "int NULL DEFAULT 0 COMMENT '新闻转发次数'");
        addColumnIfAbsent("t_post", "topics", "varchar(500) NULL COMMENT '帖子话题标签'");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_user_blacklist (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "blocked_user_id INT NOT NULL, " +
                "create_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "UNIQUE KEY uk_user_blacklist_pair (user_id, blocked_user_id), " +
                "INDEX idx_user_blacklist_user (user_id, deleted)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_user_daily_task (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "task_date DATE NOT NULL, " +
                "task_type VARCHAR(32) NOT NULL, " +
                "action_count INT NULL DEFAULT 0, " +
                "exp_gained INT NULL DEFAULT 0, " +
                "created_time DATETIME NULL, " +
                "updated_time DATETIME NULL, " +
                "UNIQUE KEY uk_daily_task_user_type (user_id, task_date, task_type), " +
                "INDEX idx_daily_task_user_date (user_id, task_date)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_news_comment (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "news_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "content TEXT NULL, " +
                "image_url VARCHAR(500) NULL, " +
                "parent_id INT NULL, " +
                "reply_user_id INT NULL, " +
                "create_time DATETIME NULL, " +
                "update_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "INDEX idx_news_comment_news (news_id, deleted, parent_id), " +
                "INDEX idx_news_comment_parent (parent_id, deleted)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_notification (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "actor_id INT NULL, " +
                "type VARCHAR(32) NOT NULL, " +
                "title VARCHAR(120) NULL, " +
                "content VARCHAR(500) NULL, " +
                "target_type VARCHAR(32) NULL, " +
                "target_id INT NULL, " +
                "target_url VARCHAR(500) NULL, " +
                "read_flag TINYINT(1) NULL DEFAULT 0, " +
                "create_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "INDEX idx_notification_user_read (user_id, read_flag, deleted, create_time), " +
                "INDEX idx_notification_target (target_type, target_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    /**
     * 在功能初始化时补齐缺失字段，保证旧库也能运行新功能。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @param definition definition 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addColumnIfAbsent(String tableName, String columnName, String definition) {
        if (!tableExists(tableName)) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count == 0) {
            log.info("Adding missing {}.{} column", tableName, columnName);
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    /**
     * 给三类账号表补齐同名字段，保持账号结构一致。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @param definition definition 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addAccountColumnIfAbsent(String columnName, String definition) {
        addColumnIfAbsent("t_player_user", columnName, definition);
        addColumnIfAbsent("t_moderator_user", columnName, definition);
        addColumnIfAbsent("t_admin_user", columnName, definition);
    }

    /**
     * 检查当前数据库是否存在目标表，避免重复执行 DDL。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示社区管理当前状态满足业务判断。
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
