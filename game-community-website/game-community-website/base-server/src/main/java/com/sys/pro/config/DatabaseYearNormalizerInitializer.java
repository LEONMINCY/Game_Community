package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 启动时把演示数据中的 2025 年时间统一顺延到 2026 年。
 * <p>
 * 这个组件只处理当前数据库中的时间字段，避免页面上出现旧年份测试数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseYearNormalizerInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        normalizeTemporalColumns();
        normalizeStringTimeColumns();
    }

    /**
     * 把时间类型字段中的历史 2025 年数据修正为 2026 年。
     */
    private void normalizeTemporalColumns() {
        queryColumns("date", "datetime", "timestamp", "year").forEach(column -> {
            String tableName = column.getTableName();
            String columnName = column.getColumnName();
            String dataType = column.getDataType();
            try {
                String sql;
                if ("year".equals(dataType)) {
                    sql = "UPDATE " + quote(tableName) + " SET " + quote(columnName) + " = 2026 WHERE "
                            + quote(columnName) + " = 2025";
                } else {
                    sql = "UPDATE " + quote(tableName) + " SET " + quote(columnName)
                            + " = DATE_ADD(" + quote(columnName) + ", INTERVAL 1 YEAR) WHERE "
                            + quote(columnName) + " >= '2025-01-01' AND " + quote(columnName) + " < '2026-01-01'";
                }
                int changed = jdbcTemplate.update(sql);
                if (changed > 0) {
                    log.info("Normalized 2025 year values for {}.{}, changed={}", tableName, columnName, changed);
                }
            } catch (Exception e) {
                log.warn("Skip normalizing year for {}.{}", tableName, columnName, e);
            }
        });
    }

    /**
     * 修正字符串时间字段中的年份文本，保持测试数据年份一致。
     */
    private void normalizeStringTimeColumns() {
        queryColumns("char", "varchar", "text", "mediumtext", "longtext").stream()
                .filter(column -> isTimeLikeColumn(column.getColumnName()))
                .forEach(column -> {
                    String tableName = column.getTableName();
                    String columnName = column.getColumnName();
                    try {
                        String sql = "UPDATE " + quote(tableName) + " SET " + quote(columnName)
                                + " = CONCAT('2026', SUBSTRING(" + quote(columnName) + ", 5)) WHERE "
                                + quote(columnName) + " LIKE '2025-%' OR "
                                + quote(columnName) + " LIKE '2025/%' OR "
                                + quote(columnName) + " LIKE '2025 %' OR "
                                + quote(columnName) + " LIKE '2025T%'";
                        int changed = jdbcTemplate.update(sql);
                        if (changed > 0) {
                            log.info("Normalized string time year for {}.{}, changed={}", tableName, columnName, changed);
                        }
                    } catch (Exception e) {
                        log.warn("Skip normalizing string time year for {}.{}", tableName, columnName, e);
                    }
                });
    }

    /**
     * 从 information_schema 查询指定数据类型的字段清单。
     * @param dataTypes dataTypes 字段，来源于当前接口入参或内部调用上下文。
     * @return 数据库结构列表数据。
     */
    private List<ColumnSpec> queryColumns(String... dataTypes) {
        String placeholders = String.join(",", java.util.Collections.nCopies(dataTypes.length, "?"));
        String sql = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND DATA_TYPE IN (" + placeholders + ")";
        return jdbcTemplate.query(sql, dataTypes, (rs, rowNum) -> new ColumnSpec(
                rs.getString("TABLE_NAME"),
                rs.getString("COLUMN_NAME"),
                rs.getString("DATA_TYPE").toLowerCase(Locale.ROOT)));
    }

    /**
     * 判断数据库结构当前状态是否满足业务条件。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示数据库结构当前状态满足业务判断。
     */
    private boolean isTimeLikeColumn(String columnName) {
        if (!StringUtils.hasText(columnName)) {
            return false;
        }
        String normalized = columnName.toLowerCase(Locale.ROOT);
        return normalized.contains("time") || normalized.contains("date");
    }

    /**
     * 为表名或字段名添加反引号，避免关键字和特殊字符破坏 SQL。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @return 数据库结构处理后的文本结果。
     */
    private String quote(String name) {
        return "`" + String.valueOf(name).replace("`", "``") + "`";
    }

    /**
     * 保存 information_schema 中的一列元数据，供年份修正逻辑逐列处理。
     */
    private static class ColumnSpec {
        private final String tableName;
        private final String columnName;
        private final String dataType;

        /**
         * 保存 information_schema 查询出来的字段元数据。
         *
         * @param tableName 字段所属表名。
         * @param columnName 字段名称。
         * @param dataType MySQL 字段类型，用来区分时间字段和字符串字段。
         */
        private ColumnSpec(String tableName, String columnName, String dataType) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.dataType = dataType;
        }

        /**
         * 读取数据库结构的 TableName 数据，供页面展示或后续业务判断。
         * @return 数据库结构处理后的文本结果。
         */
        private String getTableName() {
            return tableName;
        }

        /**
         * 读取数据库结构的 ColumnName 数据，供页面展示或后续业务判断。
         * @return 数据库结构处理后的文本结果。
         */
        private String getColumnName() {
            return columnName;
        }

        /**
         * 读取数据库结构的 DataType 数据，供页面展示或后续业务判断。
         * @return 数据库结构处理后的文本结果。
         */
        private String getDataType() {
            return dataType;
        }
    }
}
