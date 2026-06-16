package com.sys.pro.config;

import com.sys.pro.pojo.User;
import com.sys.pro.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 社区审核员初始化组件，创建审核员角色、菜单和默认账号数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityModeratorInitializer implements CommandLineRunner {

    private static final long PREFERRED_MODERATOR_ROLE_ID = 3L;
    private static final String DEFAULT_MODERATOR_USERNAME = "moderator";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final String DEFAULT_AVATAR = "https://c-ssl.dtstatic.com/uploads/item/202106/29/20210629000609_JCBXJ.thumb.1000_0.jpeg";

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 应用启动时检查并补齐运行所需的数据结构或基础数据。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    @Override
    public void run(String... args) {
        ensureMenuTables();
        Long roleId = ensureModeratorRole();
        ensureRoleMenu(roleId, ensureMenu("Histogram", "/admin/index", "首页", 1));
        ensureRoleMenu(roleId, ensureMenu("TrendCharts", "/admin/adminNews", "新闻管理", 4));
        ensureRoleMenu(roleId, ensureMenu("Promotion", "/admin/post", "帖子管理", 5));
        ensureRoleMenu(roleId, ensureMenu("Warning", "/admin/report", "举报管理", 6));
        ensureRoleMenu(roleId, ensureMenu("Document", "/admin/order", "订单管理", 7));
        ensureModeratorAccount(roleId);
        log.info("Community moderator role checked, roleId={}", roleId);
    }

    /**
     * 确保社区管理依赖的数据或结构存在，避免运行时缺少基础配置。
     */
    private void ensureMenuTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_menu (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "icon VARCHAR(64) NULL, " +
                "`index` VARCHAR(100) NOT NULL, " +
                "title VARCHAR(100) NOT NULL, " +
                "sort INT NULL DEFAULT 0, " +
                "created_time DATETIME NULL, " +
                "updated_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "UNIQUE KEY uk_t_menu_index (`index`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS t_role_menu (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "role_id BIGINT NOT NULL, " +
                "menu_id BIGINT NOT NULL, " +
                "created_time DATETIME NULL, " +
                "updated_time DATETIME NULL, " +
                "deleted TINYINT(1) NULL DEFAULT 0, " +
                "UNIQUE KEY uk_t_role_menu (role_id, menu_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    /**
     * 确保社区管理依赖的数据或结构存在，避免运行时缺少基础配置。
     * @return 社区管理统计值或主键结果。
     */
    private Long ensureModeratorRole() {
        Long id3Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_role WHERE id = ?",
                Long.class,
                PREFERRED_MODERATOR_ROLE_ID
        );
        if (id3Count != null && id3Count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO t_role (id, role_name, role_description, created_time, updated_time, deleted) VALUES (?, ?, ?, NOW(), NOW(), 0)",
                    PREFERRED_MODERATOR_ROLE_ID,
                    "moderator",
                    "社区审核员"
            );
            return PREFERRED_MODERATOR_ROLE_ID;
        }

        jdbcTemplate.update(
                "UPDATE t_role SET role_name = ?, role_description = ?, updated_time = NOW(), deleted = 0 WHERE id = ?",
                "moderator",
                "社区审核员",
                PREFERRED_MODERATOR_ROLE_ID
        );
        return PREFERRED_MODERATOR_ROLE_ID;
    }

    /**
     * 确保社区管理依赖的数据或结构存在，避免运行时缺少基础配置。
     * @param roleId role 主键，用来定位关联业务数据。
     */
    private void ensureModeratorAccount(Long roleId) {
        User user = userService.findByUsername(DEFAULT_MODERATOR_USERNAME);
        Integer userId;
        if (user == null) {
            user = new User();
            user.setUsername(DEFAULT_MODERATOR_USERNAME);
            user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            user.setRoleId(roleId);
            user.setEnableFlag(true);
            user.setDeleted(false);
            userService.save(user);
            userId = user.getId();
        } else {
            userId = user.getId();
            User patch = new User();
            patch.setId(userId);
            patch.setRoleId(roleId);
            patch.setEnableFlag(true);
            patch.setDeleted(false);
            userService.updateById(patch);
        }

        Long infoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user_info WHERE user_id = ?",
                Long.class,
                userId
        );
        if (infoCount != null && infoCount > 0) {
            jdbcTemplate.update(
                    "UPDATE t_user_info SET nickname = ?, avatar = COALESCE(NULLIF(avatar, ''), ?), ex1 = COALESCE(ex1, 0), ex2 = COALESCE(ex2, 0), deleted = 0 WHERE user_id = ?",
                    "社区审核员",
                    DEFAULT_AVATAR,
                    userId
            );
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO t_user_info (id, user_id, nickname, avatar, ex1, ex2, deleted) VALUES (?, ?, ?, ?, 0, 0, 0)",
                userId,
                userId,
                "社区审核员",
                DEFAULT_AVATAR
        );
    }

    /**
     * 确保社区管理依赖的数据或结构存在，避免运行时缺少基础配置。
     * @param icon icon 字段，来源于当前接口入参或内部调用上下文。
     * @param index index 字段，来源于当前接口入参或内部调用上下文。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param sort sort 字段，来源于当前接口入参或内部调用上下文。
     * @return 社区管理统计值或主键结果。
     */
    private Long ensureMenu(String icon, String index, String title, int sort) {
        List<Long> menuIds = jdbcTemplate.queryForList(
                "SELECT id FROM t_menu WHERE `index` = ? AND deleted = 0 LIMIT 1",
                Long.class,
                index
        );
        if (!menuIds.isEmpty()) {
            return menuIds.get(0);
        }

        jdbcTemplate.update(
                "INSERT INTO t_menu (icon, `index`, title, sort, created_time, updated_time, deleted) VALUES (?, ?, ?, ?, NOW(), NOW(), 0)",
                icon,
                index,
                title,
                sort
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM t_menu WHERE `index` = ? AND deleted = 0 ORDER BY id DESC LIMIT 1",
                Long.class,
                index
        );
    }

    /**
     * 确保社区管理依赖的数据或结构存在，避免运行时缺少基础配置。
     * @param roleId role 主键，用来定位关联业务数据。
     * @param menuId menu 主键，用来定位关联业务数据。
     */
    private void ensureRoleMenu(Long roleId, Long menuId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_role_menu WHERE role_id = ? AND menu_id = ?",
                Long.class,
                roleId,
                menuId
        );
        if (count != null && count > 0) {
            jdbcTemplate.update(
                    "UPDATE t_role_menu SET deleted = 0, updated_time = NOW() WHERE role_id = ? AND menu_id = ?",
                    roleId,
                    menuId
            );
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO t_role_menu (role_id, menu_id, created_time, updated_time, deleted) VALUES (?, ?, NOW(), NOW(), 0)",
                roleId,
                menuId
        );
    }
}
