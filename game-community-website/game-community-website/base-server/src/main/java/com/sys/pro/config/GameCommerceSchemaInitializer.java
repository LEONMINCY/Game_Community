package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 游戏交易表结构初始化组件，补齐购物车、愿望单、订单和退款字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameCommerceSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_game_wishlist (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "game_id INT NOT NULL, " +
                "create_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "UNIQUE KEY uk_wishlist_user_game (user_id, game_id), " +
                "INDEX idx_wishlist_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_game_cart (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "game_id INT NOT NULL, " +
                "create_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "UNIQUE KEY uk_cart_user_game (user_id, game_id), " +
                "INDEX idx_cart_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addOrderColumnIfAbsent("refund_status", "varchar(20) NULL DEFAULT 'NONE' COMMENT '退款状态'");
        addOrderColumnIfAbsent("refund_reason", "varchar(500) NULL COMMENT '退款原因'");
        addOrderColumnIfAbsent("refund_reply", "varchar(500) NULL COMMENT '退款审核回复'");
        addOrderColumnIfAbsent("refund_apply_time", "datetime NULL COMMENT '退款申请时间'");
        addOrderColumnIfAbsent("refund_review_time", "datetime NULL COMMENT '退款审核时间'");
        jdbcTemplate.execute("UPDATE t_order SET refund_status = 'NONE' WHERE refund_status IS NULL OR refund_status = ''");
        jdbcTemplate.execute("UPDATE t_order SET deleted = 0 WHERE deleted IS NULL");
    }

    /**
     * 给订单表补齐支付和退款流程需要的字段。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @param definition definition 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addOrderColumnIfAbsent(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_order' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            log.info("Adding missing t_order.{} column", columnName);
            jdbcTemplate.execute("ALTER TABLE t_order ADD COLUMN " + columnName + " " + definition);
        }
    }
}
