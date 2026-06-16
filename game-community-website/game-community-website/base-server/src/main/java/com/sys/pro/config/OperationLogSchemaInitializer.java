package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 操作日志表结构初始化组件，确保日志管理页面需要的字段存在。
 */
@Component
@Order(40)
@RequiredArgsConstructor
public class OperationLogSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_operation_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NULL," +
                "username VARCHAR(64) NULL," +
                "role_id INT NULL," +
                "role_name VARCHAR(32) NULL," +
                "module_name VARCHAR(80) NULL," +
                "operation_name VARCHAR(160) NULL," +
                "controller_name VARCHAR(120) NULL," +
                "method_name VARCHAR(120) NULL," +
                "request_method VARCHAR(16) NULL," +
                "request_uri VARCHAR(255) NULL," +
                "request_params TEXT NULL," +
                "ip_address VARCHAR(64) NULL," +
                "success TINYINT(1) NOT NULL DEFAULT 1," +
                "error_message VARCHAR(1000) NULL," +
                "cost_time BIGINT NULL," +
                "created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "deleted TINYINT(1) NOT NULL DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        addIndex("idx_operation_log_time", "CREATE INDEX idx_operation_log_time ON t_operation_log(created_time)");
        addIndex("idx_operation_log_user", "CREATE INDEX idx_operation_log_user ON t_operation_log(user_id, created_time)");
        addIndex("idx_operation_log_role", "CREATE INDEX idx_operation_log_role ON t_operation_log(role_id, created_time)");
        addIndex("idx_operation_log_success", "CREATE INDEX idx_operation_log_success ON t_operation_log(success, created_time)");
    }

    /**
     * 创建指定索引，提升后台筛选和前台查询速度。
     * @param indexName indexName 字段，来源于当前接口入参或内部调用上下文。
     * @param sql sql 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addIndex(String indexName, String sql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 't_operation_log' AND index_name = ?",
                Integer.class,
                indexName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }
}
