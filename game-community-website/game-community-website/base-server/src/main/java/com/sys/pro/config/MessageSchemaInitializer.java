package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 私信表结构初始化器，负责为旧数据库补齐未读消息统计所需字段和索引。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查私信表结构，确保未读消息功能可以在旧库上直接运行。
     * @param args Spring Boot 启动参数，本初始化器不直接使用。
     */
    @Override
    public void run(String... args) {
        if (!tableExists("t_message")) {
            return;
        }
        addColumnIfAbsent(
                "read_flag",
                "TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否已读：0-未读，1-已读'");
        jdbcTemplate.execute("UPDATE t_message SET read_flag = 1 WHERE read_flag IS NULL");
        ensureReadFlagDefaultUnread();
        addIndexIfAbsent(
                "idx_message_receiver_read",
                "CREATE INDEX idx_message_receiver_read ON t_message(receiver_id, read_flag, sender_id, deleted)");
        log.info("Message schema checked");
    }

    /**
     * 判断目标表是否存在，避免初始化器在缺少基础表时打断应用启动。
     * @param tableName 需要检查的表名。
     * @return true 表示当前数据库中存在该表。
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName);
        return count != null && count > 0;
    }

    /**
     * 只在字段缺失时执行 ALTER TABLE，兼容已经升级过的数据库。
     * @param columnName 需要补齐的字段名。
     * @param definition 字段类型、默认值和注释定义。
     */
    private void addColumnIfAbsent(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_message' AND COLUMN_NAME = ?",
                Integer.class,
                columnName);
        if (count != null && count == 0) {
            log.info("Adding missing t_message.{} column", columnName);
            jdbcTemplate.execute("ALTER TABLE t_message ADD COLUMN " + columnName + " " + definition);
        }
    }

    /**
     * 将新插入私信的数据库默认值保持为未读，兼容旧库首次补字段时历史消息默认已读的迁移策略。
     */
    private void ensureReadFlagDefaultUnread() {
        String columnDefault = jdbcTemplate.queryForObject(
                "SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_message' AND COLUMN_NAME = 'read_flag'",
                String.class);
        if (!"0".equals(String.valueOf(columnDefault))) {
            jdbcTemplate.execute("ALTER TABLE t_message MODIFY COLUMN read_flag TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读'");
        }
    }

    /**
     * 为未读统计和按会话清零建立复合索引，减少消息中心进入时的查询压力。
     * @param indexName 需要检查的索引名。
     * @param ddl 创建索引的 SQL。
     */
    private void addIndexIfAbsent(String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_message' AND INDEX_NAME = ?",
                Integer.class,
                indexName);
        if (count != null && count == 0) {
            log.info("Adding missing t_message index {}", indexName);
            jdbcTemplate.execute(ddl);
        }
    }
}
