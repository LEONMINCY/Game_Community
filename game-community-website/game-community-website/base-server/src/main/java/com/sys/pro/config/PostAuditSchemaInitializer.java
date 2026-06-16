package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/**
 * 帖子审核表结构初始化组件，补齐审核、拒绝和申诉流程字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostAuditSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        addColumnIfAbsent("audit_status", "varchar(20) NULL DEFAULT 'approved' COMMENT '审核状态'");
        addColumnIfAbsent("audit_reason", "varchar(500) NULL COMMENT '审核拒绝原因'");
        addColumnIfAbsent("appeal_content", "text NULL COMMENT '申诉内容'");
        addColumnIfAbsent("appeal_status", "varchar(20) NULL DEFAULT 'none' COMMENT '申诉状态'");
        addColumnIfAbsent("appeal_reply", "varchar(500) NULL COMMENT '申诉处理回复'");
        addColumnIfAbsent("review_time", "datetime NULL COMMENT '审核时间'");
        addColumnIfAbsent("appeal_time", "datetime NULL COMMENT '申诉时间'");
        addColumnIfAbsent("share_count", "int NULL DEFAULT 0 COMMENT '转发次数'");
        jdbcTemplate.execute("UPDATE t_post SET audit_status = 'approved' WHERE audit_status IS NULL OR audit_status = ''");
        jdbcTemplate.execute("UPDATE t_post SET appeal_status = 'none' WHERE appeal_status IS NULL OR appeal_status = ''");
        jdbcTemplate.execute("UPDATE t_post SET share_count = 0 WHERE share_count IS NULL");
    }

    /**
     * 在功能初始化时补齐缺失字段，保证旧库也能运行新功能。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @param definition definition 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addColumnIfAbsent(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_post' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count == 0) {
            log.info("Adding missing t_post.{} column", columnName);
            jdbcTemplate.execute("ALTER TABLE t_post ADD COLUMN " + columnName + " " + definition);
        }
    }
}
