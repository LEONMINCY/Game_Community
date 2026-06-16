package com.sys.pro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐玩家、审核员和管理员账号表，并把旧用户数据迁移到对应账号表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        createAccountTables();
        migrateLegacyUsers();
        syncAccountSequence();
    }

    /**
     * 创建管理员、审核员和用户三类账号表，保证账号按角色分开存储。
     */
    private void createAccountTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_account_sequence (" +
                "id INT AUTO_INCREMENT PRIMARY KEY" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        createAccountTable("t_player_user");
        createAccountTable("t_moderator_user");
        createAccountTable("t_admin_user");
    }

    /**
     * 按角色表结构创建单个账号表，补齐登录和资料所需字段。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     */
    private void createAccountTable(String tableName) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id INT NOT NULL PRIMARY KEY, " +
                "username VARCHAR(64) NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "role_id BIGINT NOT NULL, " +
                "enable_flag TINYINT(1) DEFAULT 1, " +
                "ban_end_time DATETIME NULL, " +
                "last_login DATETIME NULL, " +
                "created_time DATETIME NULL, " +
                "updated_time DATETIME NULL, " +
                "deleted TINYINT(1) DEFAULT 0, " +
                "UNIQUE KEY uk_" + tableName + "_username (username), " +
                "KEY idx_" + tableName + "_deleted (deleted), " +
                "KEY idx_" + tableName + "_role (role_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    /**
     * 迁移旧 t_user 数据到新账号表，保留历史账号的登录能力。
     */
    private void migrateLegacyUsers() {
        if (!tableExists("t_user")) {
            return;
        }
        addColumnIfMissing("t_user", "ban_end_time", "ALTER TABLE t_user ADD COLUMN ban_end_time DATETIME NULL");
        addColumnIfMissing("t_user", "enable_flag", "ALTER TABLE t_user ADD COLUMN enable_flag TINYINT(1) DEFAULT 1");
        addColumnIfMissing("t_user", "last_login", "ALTER TABLE t_user ADD COLUMN last_login DATETIME NULL");
        addColumnIfMissing("t_user", "created_time", "ALTER TABLE t_user ADD COLUMN created_time DATETIME NULL");
        addColumnIfMissing("t_user", "updated_time", "ALTER TABLE t_user ADD COLUMN updated_time DATETIME NULL");
        addColumnIfMissing("t_user", "deleted", "ALTER TABLE t_user ADD COLUMN deleted TINYINT(1) DEFAULT 0");

        migrateRole("t_admin_user", 1);
        migrateRole("t_moderator_user", 3);
        migratePlayers();
    }

    /**
     * 按角色编号迁移账号数据，避免不同角色混在同一张表。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param roleId role 主键，用来定位关联业务数据。
     */
    private void migrateRole(String tableName, int roleId) {
        jdbcTemplate.update("INSERT IGNORE INTO " + tableName +
                        " (id, username, password, role_id, enable_flag, ban_end_time, last_login, created_time, updated_time, deleted) " +
                        "SELECT id, username, password, role_id, COALESCE(enable_flag, 1), ban_end_time, last_login, created_time, updated_time, COALESCE(deleted, 0) " +
                        "FROM t_user WHERE role_id = ?",
                roleId);
    }

    /**
     * 迁移没有明确后台角色的普通玩家账号。
     */
    private void migratePlayers() {
        jdbcTemplate.update("INSERT IGNORE INTO t_player_user " +
                " (id, username, password, role_id, enable_flag, ban_end_time, last_login, created_time, updated_time, deleted) " +
                "SELECT id, username, password, 2, COALESCE(enable_flag, 1), ban_end_time, last_login, created_time, updated_time, COALESCE(deleted, 0) " +
                "FROM t_user WHERE role_id IS NULL OR role_id NOT IN (1, 3)");
    }

    /**
     * 同步账号主键序列，避免迁移后新增账号出现主键冲突。
     */
    private void syncAccountSequence() {
        Integer maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM (" +
                        "SELECT id FROM t_player_user UNION ALL " +
                        "SELECT id FROM t_moderator_user UNION ALL " +
                        "SELECT id FROM t_admin_user UNION ALL " +
                        "SELECT id FROM t_account_sequence" +
                        ") ids",
                Integer.class
        );
        int nextId = (maxId == null ? 0 : maxId) + 1;
        jdbcTemplate.execute("ALTER TABLE t_account_sequence AUTO_INCREMENT = " + Math.max(nextId, 1));
        log.info("Account tables checked in database {}, next account id starts from {}", currentDatabase(), nextId);
    }

    /**
     * 在表结构缺少字段时补列，兼容旧数据库直接启动。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @param ddl ddl 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    /**
     * 检查当前数据库是否存在目标表，避免重复执行 DDL。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
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
     * 检查目标字段是否已经存在，决定是否需要执行补列语句。
     * @param tableName tableName 字段，来源于当前接口入参或内部调用上下文。
     * @param columnName columnName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
     */
    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    /**
     * 读取当前连接使用的数据库名称，用于 information_schema 查询。
     * @return 账号表处理后的文本结果。
     */
    private String currentDatabase() {
        return jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
    }
}
