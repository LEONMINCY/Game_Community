package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * AI 助手表结构初始化组件，补齐对话和消息表所需字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAssistantSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_ai_conversation (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "title VARCHAR(100) NULL, " +
                "create_time DATETIME NULL, " +
                "update_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "INDEX idx_ai_conversation_user_update (user_id, update_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_ai_message (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "conversation_id BIGINT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "role VARCHAR(20) NOT NULL, " +
                "content LONGTEXT NOT NULL, " +
                "image_urls LONGTEXT NULL, " +
                "create_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "INDEX idx_ai_message_conversation_time (conversation_id, create_time), " +
                "INDEX idx_ai_message_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addColumnIfAbsent("t_ai_message", "image_urls", "LONGTEXT NULL COMMENT 'AI消息图片URL JSON'");
        log.info("AI assistant schema checked");
    }

    /**
     * 在功能初始化时补齐缺失字段，保证旧库也能运行新功能。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @param definition definition 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addColumnIfAbsent(String tableName, String columnName, String definition) {
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
}
