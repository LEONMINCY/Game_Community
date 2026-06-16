package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 游戏扩展表结构初始化组件，补齐游戏类型、平台、折扣和媒体字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameFeatureSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        addColumnIfAbsent("t_game", "discount", "int NULL DEFAULT 0 COMMENT '折扣百分比'");
        addColumnIfAbsent("t_game", "price_mark", "varchar(32) NULL COMMENT '价格标记：historical_low/tie_historical_low'");
        addColumnIfAbsent("t_game", "release_date", "date NULL COMMENT '发售日期'");
        addColumnIfAbsent("t_game_rating", "content", "text NULL COMMENT '评价内容'");
        addColumnIfAbsent("t_game_rating", "recommend", "tinyint(1) NULL COMMENT '是否好评'");
        addColumnIfAbsent("t_user_info", "signature", "varchar(120) NULL COMMENT '个性签名'");
        addColumnIfAbsent("t_game", "platforms", "varchar(255) NULL COMMENT 'supported game platforms'");
        jdbcTemplate.execute("UPDATE t_game SET discount = 0 WHERE discount IS NULL");
        jdbcTemplate.execute("UPDATE t_game SET release_date = DATE(create_time) WHERE release_date IS NULL AND create_time IS NOT NULL");
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
