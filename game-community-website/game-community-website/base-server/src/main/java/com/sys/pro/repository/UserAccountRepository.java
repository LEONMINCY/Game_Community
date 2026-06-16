package com.sys.pro.repository;

import com.sys.pro.common.PageResult;
import com.sys.pro.dto.UserPageDTO;
import com.sys.pro.pojo.User;
import com.sys.pro.vo.UserPageVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UserAccountRepository 封装数据库读写。
 */
@Repository
@RequiredArgsConstructor
public class UserAccountRepository {

    public static final long ROLE_ADMIN = 1L;
    public static final long ROLE_PLAYER = 2L;
    public static final long ROLE_MODERATOR = 3L;

    private static final String PLAYER_TABLE = "t_player_user";
    private static final String MODERATOR_TABLE = "t_moderator_user";
    private static final String ADMIN_TABLE = "t_admin_user";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 按账号主键查询跨角色账号资料。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 账号表在该步骤产出的业务结果。
     */
    public User findById(Serializable id) {
        if (id == null) {
            return null;
        }
        List<User> users = jdbcTemplate.query(accountUnionSql("WHERE id = ? AND deleted = 0"), userRowMapper(), id);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 按用户名查询账号，支持登录和注册重复校验。
     * @param username username 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表在该步骤产出的业务结果。
     */
    public User findByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        List<User> users = jdbcTemplate.query(accountUnionSql("WHERE username = ? AND deleted = 0"), userRowMapper(), username.trim());
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 通过用户名、手机号或绑定邮箱定位登录账号，支持登录和找回密码共用同一套账号入口。
     * @param identifier 用户输入的账号标识，可以是用户名、手机号或邮箱。
     * @return 匹配到的账号；不存在时返回 null。
     */
    public User findByIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return null;
        }
        String keyword = identifier.trim();
        String sql = "SELECT account.* FROM (" +
                "SELECT * FROM t_admin_user UNION ALL " +
                "SELECT * FROM t_moderator_user UNION ALL " +
                "SELECT * FROM t_player_user" +
                ") account " +
                "LEFT JOIN t_user_info ui ON ui.user_id = account.id " +
                "WHERE account.deleted = 0 AND COALESCE(ui.deleted, 0) = 0 " +
                "AND (account.username = ? OR ui.phone = ? OR ui.email = ?) LIMIT 1";
        List<User> users = jdbcTemplate.query(sql, userRowMapper(), keyword, keyword, keyword);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 判断用户名是否已被占用，避免注册重复账号。
     * @param username username 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
     */
    public boolean existsUsername(String username) {
        return findByUsername(username) != null;
    }

    /**
     * 统计三类账号表中的总账号数量。
     * @return 账号表统计值或主键结果。
     */
    public long countAll() {
        Long total = jdbcTemplate.queryForObject(
                "SELECT SUM(cnt) FROM (" +
                        "SELECT COUNT(*) cnt FROM t_player_user WHERE deleted = 0 " +
                        "UNION ALL SELECT COUNT(*) cnt FROM t_moderator_user WHERE deleted = 0 " +
                        "UNION ALL SELECT COUNT(*) cnt FROM t_admin_user WHERE deleted = 0" +
                        ") x",
                Long.class
        );
        return total == null ? 0L : total;
    }

    /**
     * 统计指定时间之后新增的账号数量。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表统计值或主键结果。
     */
    public long countCreatedAfter(LocalDateTime start) {
        return countByTimeRange("created_time", start, null);
    }

    /**
     * 统计指定时间区间内新增的账号数量。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表统计值或主键结果。
     */
    public long countCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return countByTimeRange("created_time", start, end);
    }

    /**
     * 统计最近登录过的账号数量，用于活跃度分析。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表统计值或主键结果。
     */
    public long countLastLoginAfter(LocalDateTime start) {
        return countByTimeRange("last_login", start, null);
    }

    /**
     * 把新账号写入对应角色账号表。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
     */
    public boolean insert(User user) {
        if (user.getId() == null) {
            user.setId(nextGlobalId());
        }
        if (user.getRoleId() == null) {
            user.setRoleId(ROLE_PLAYER);
        }
        LocalDateTime now = LocalDateTime.now();
        if (user.getCreatedTime() == null) {
            user.setCreatedTime(now);
        }
        if (user.getUpdatedTime() == null) {
            user.setUpdatedTime(now);
        }
        if (user.getEnableFlag() == null) {
            user.setEnableFlag(true);
        }
        if (user.getDeleted() == null) {
            user.setDeleted(false);
        }

        String sql = "INSERT INTO " + tableForRole(user.getRoleId()) +
                " (id, username, password, role_id, enable_flag, ban_end_time, last_login, created_time, updated_time, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRoleId(),
                user.getEnableFlag(),
                toTimestamp(user.getBanEndTime()),
                toTimestamp(user.getLastLogin()),
                toTimestamp(user.getCreatedTime()),
                toTimestamp(user.getUpdatedTime()),
                user.getDeleted()
        ) > 0;
    }

    /**
     * 按账号主键更新对应角色表中的账号信息。
     * @param patch patch 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
     */
    public boolean updateById(User patch) {
        if (patch == null || patch.getId() == null) {
            return false;
        }
        User existing = findById(patch.getId());
        if (existing == null) {
            return false;
        }
        Long targetRole = patch.getRoleId() == null ? existing.getRoleId() : patch.getRoleId();
        if (!targetRole.equals(existing.getRoleId())) {
            return moveRole(existing, patch, targetRole);
        }
        return updateSameTable(patch, tableForRole(existing.getRoleId()));
    }

    /**
     * 按账号主键软删除账号，避免物理删除破坏历史数据。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return true 表示账号表当前状态满足业务判断。
     */
    public boolean softDeleteById(Serializable id) {
        User existing = findById(id);
        if (existing == null) {
            return false;
        }
        return jdbcTemplate.update("UPDATE " + tableForRole(existing.getRoleId()) + " SET deleted = 1, updated_time = NOW() WHERE id = ?", id) > 0;
    }

    /**
     * 修改当前用户密码，校验旧密码后写入新哈希。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param encodedPassword encodedPassword 字段，来源于当前接口入参或内部调用上下文。
     */
    public void updatePassword(Integer userId, String encodedPassword) {
        User existing = findById(userId);
        if (existing == null) {
            return;
        }
        jdbcTemplate.update("UPDATE " + tableForRole(existing.getRoleId()) + " SET password = ?, updated_time = NOW() WHERE id = ?", encodedPassword, userId);
    }

    /**
     * 转换账号表字段格式，便于后续计算或接口返回。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    public void touchLastLogin(Integer userId) {
        User existing = findById(userId);
        if (existing == null) {
            return;
        }
        jdbcTemplate.update("UPDATE " + tableForRole(existing.getRoleId()) + " SET last_login = NOW(), updated_time = NOW() WHERE id = ?", userId);
    }

    /**
     * 完成账号表中的 pageUsers 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 账号表分页结果，包含当前页数据和总数。
     */
    public PageResult<UserPageVo> pageUsers(UserPageDTO dto) {
        List<Object> params = new ArrayList<>();
        String where = buildPageWhere(dto, params);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + pageBaseSql() + ") q " + where, Long.class, params.toArray());

        int pageNo = dto.getPageNo() == null ? 1 : dto.getPageNo();
        int pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();
        params.add(Math.max(0, (pageNo - 1) * pageSize));
        params.add(pageSize);

        List<UserPageVo> list = jdbcTemplate.query(
                "SELECT * FROM (" + pageBaseSql() + ") q " + where + " ORDER BY created_time DESC, id DESC LIMIT ?, ?",
                userPageRowMapper(),
                params.toArray()
        );
        return new PageResult<>(list, total == null ? 0L : total);
    }

    /**
     * 在角色变更时把账号数据移动到目标角色表。
     * @param existing existing 字段，来源于当前接口入参或内部调用上下文。
     * @param patch patch 字段，来源于当前接口入参或内部调用上下文。
     * @param targetRole targetRole 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
     */
    private boolean moveRole(User existing, User patch, Long targetRole) {
        User merged = merge(existing, patch);
        merged.setRoleId(targetRole);
        String targetTable = tableForRole(targetRole);
        jdbcTemplate.update("UPDATE " + tableForRole(existing.getRoleId()) + " SET deleted = 1, updated_time = NOW() WHERE id = ?", existing.getId());
        if (accountExistsInTable(targetTable, existing.getId())) {
            return updateSameTable(merged, targetTable);
        }
        return insert(merged);
    }

    /**
     * 合并账号更新字段，只覆盖本次明确提交的内容。
     * @param existing existing 字段，来源于当前接口入参或内部调用上下文。
     * @param patch patch 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表在该步骤产出的业务结果。
     */
    private User merge(User existing, User patch) {
        User merged = new User();
        merged.setId(existing.getId());
        merged.setUsername(StringUtils.defaultIfBlank(patch.getUsername(), existing.getUsername()));
        merged.setPassword(StringUtils.defaultIfBlank(patch.getPassword(), existing.getPassword()));
        merged.setRoleId(patch.getRoleId() == null ? existing.getRoleId() : patch.getRoleId());
        merged.setEnableFlag(patch.getEnableFlag() == null ? existing.getEnableFlag() : patch.getEnableFlag());
        merged.setBanEndTime(patch.getBanEndTime() == null ? existing.getBanEndTime() : patch.getBanEndTime());
        merged.setLastLogin(patch.getLastLogin() == null ? existing.getLastLogin() : patch.getLastLogin());
        merged.setCreatedTime(existing.getCreatedTime());
        merged.setUpdatedTime(LocalDateTime.now());
        merged.setDeleted(false);
        return merged;
    }

    /**
     * 在同一角色表内更新账号字段。
     * @param patch patch 字段，来源于当前接口入参或内部调用上下文。
     * @param table table 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示账号表当前状态满足业务判断。
     */
    private boolean updateSameTable(User patch, String table) {
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();
        addSet(sets, params, "username", patch.getUsername());
        addSet(sets, params, "password", patch.getPassword());
        addSet(sets, params, "role_id", patch.getRoleId());
        addSet(sets, params, "enable_flag", patch.getEnableFlag());
        addSet(sets, params, "ban_end_time", patch.getBanEndTime());
        if (Boolean.TRUE.equals(patch.getEnableFlag()) && patch.getBanEndTime() == null) {
            sets.add("ban_end_time = NULL");
        }
        addSet(sets, params, "last_login", patch.getLastLogin());
        addSet(sets, params, "deleted", patch.getDeleted());
        sets.add("updated_time = NOW()");
        params.add(patch.getId());
        return jdbcTemplate.update("UPDATE " + table + " SET " + String.join(", ", sets) + " WHERE id = ?", params.toArray()) > 0;
    }

    /**
     * 拼接账号更新 SQL 的 SET 字段和值。
     * @param sets sets 字段，来源于当前接口入参或内部调用上下文。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @param column column 字段，来源于当前接口入参或内部调用上下文。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     */
    private void addSet(List<String> sets, List<Object> params, String column, Object value) {
        if (value == null) {
            return;
        }
        sets.add(column + " = ?");
        params.add(value instanceof LocalDateTime ? toTimestamp((LocalDateTime) value) : value);
    }

    /**
     * 检查指定账号是否存在于某个角色账号表。
     * @param table table 字段，来源于当前接口入参或内部调用上下文。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return true 表示账号表当前状态满足业务判断。
     */
    private boolean accountExistsInTable(String table, Integer id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * 生成跨三类账号表唯一的账号主键。
     * @return 账号表统计值或主键结果。
     */
    private int nextGlobalId() {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO t_account_sequence VALUES ()", Statement.RETURN_GENERATED_KEYS);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            /**
             * 完成账号表中的 IllegalStateException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 账号表在该步骤产出的业务结果。
             */
            throw new IllegalStateException("Failed to generate global account id");
        }
        return key.intValue();
    }

    /**
     * 按时间范围统计账号数量，支撑后台趋势图。
     * @param column column 字段，来源于当前接口入参或内部调用上下文。
     * @param start start 字段，来源于当前接口入参或内部调用上下文。
     * @param end end 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表统计值或主键结果。
     */
    private long countByTimeRange(String column, LocalDateTime start, LocalDateTime end) {
        List<Object> params = new ArrayList<>();
        String condition = " WHERE deleted = 0";
        if (start != null) {
            condition += " AND " + column + " >= ?";
            params.add(toTimestamp(start));
        }
        if (end != null) {
            condition += " AND " + column + " < ?";
            params.add(toTimestamp(end));
        }
        List<Object> allParams = new ArrayList<>();
        allParams.addAll(params);
        allParams.addAll(params);
        allParams.addAll(params);
        Long total = jdbcTemplate.queryForObject(
                "SELECT SUM(cnt) FROM (" +
                        "SELECT COUNT(*) cnt FROM t_player_user" + condition +
                        " UNION ALL SELECT COUNT(*) cnt FROM t_moderator_user" + condition +
                        " UNION ALL SELECT COUNT(*) cnt FROM t_admin_user" + condition +
                        ") x",
                Long.class,
                allParams.toArray()
        );
        return total == null ? 0L : total;
    }

    /**
     * 根据角色编号选择对应的账号表名。
     * @param roleId role 主键，用来定位关联业务数据。
     * @return 账号表处理后的文本结果。
     */
    private String tableForRole(Long roleId) {
        if (roleId != null && roleId == ROLE_ADMIN) {
            return ADMIN_TABLE;
        }
        if (roleId != null && roleId == ROLE_MODERATOR) {
            return MODERATOR_TABLE;
        }
        return PLAYER_TABLE;
    }

    /**
     * 拼接三类账号表的 UNION 查询片段。
     * @param outerWhere outerWhere 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表处理后的文本结果。
     */
    private String accountUnionSql(String outerWhere) {
        return "SELECT * FROM (" +
                "SELECT * FROM t_admin_user UNION ALL " +
                "SELECT * FROM t_moderator_user UNION ALL " +
                "SELECT * FROM t_player_user" +
                ") accounts " + outerWhere + " LIMIT 1";
    }

    /**
     * 构建账号分页查询的基础 SQL。
     * @return 账号表处理后的文本结果。
     */
    private String pageBaseSql() {
        return "SELECT a.id, a.username, a.enable_flag, a.last_login, a.created_time, a.role_id, a.deleted, " +
                "u.nickname, u.phone, u.avatar, u.ex1, COALESCE(NULLIF(r.role_description, ''), r.role_name) role_name " +
                "FROM (" +
                "SELECT * FROM t_admin_user UNION ALL " +
                "SELECT * FROM t_moderator_user UNION ALL " +
                "SELECT * FROM t_player_user" +
                ") a " +
                "LEFT JOIN t_user_info u ON a.id = u.user_id " +
                "LEFT JOIN t_role r ON r.id = a.role_id";
    }

    /**
     * 组装账号表所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param params params 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表处理后的文本结果。
     */
    private String buildPageWhere(UserPageDTO dto, List<Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("deleted = 0");
        if (StringUtils.isNotBlank(dto.getNickname())) {
            conditions.add("nickname LIKE ?");
            params.add("%" + dto.getNickname().trim() + "%");
        }
        if (dto.getRoleId() != null) {
            conditions.add("role_id = ?");
            params.add(dto.getRoleId());
        }
        if (StringUtils.isNotBlank(dto.getPhone())) {
            conditions.add("phone LIKE ?");
            params.add("%" + dto.getPhone().trim() + "%");
        }
        if (dto.getStartTime() != null) {
            conditions.add("created_time >= ?");
            params.add(toTimestamp(dto.getStartTime()));
        }
        if (dto.getEndTime() != null) {
            conditions.add("created_time <= ?");
            params.add(toTimestamp(dto.getEndTime()));
        }
        return "WHERE " + String.join(" AND ", conditions);
    }

    /**
     * 把账号查询结果映射成用户实体。
     * @return 账号表聚合数据，键名与前端展示字段保持一致。
     */
    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setRoleId(rs.getLong("role_id"));
            user.setEnableFlag(rs.getBoolean("enable_flag"));
            Timestamp banEndTime = rs.getTimestamp("ban_end_time");
            if (banEndTime != null) {
                user.setBanEndTime(banEndTime.toLocalDateTime());
            }
            Timestamp lastLogin = rs.getTimestamp("last_login");
            if (lastLogin != null) {
                user.setLastLogin(lastLogin.toLocalDateTime());
            }
            Timestamp createdTime = rs.getTimestamp("created_time");
            if (createdTime != null) {
                user.setCreatedTime(createdTime.toLocalDateTime());
            }
            Timestamp updatedTime = rs.getTimestamp("updated_time");
            if (updatedTime != null) {
                user.setUpdatedTime(updatedTime.toLocalDateTime());
            }
            user.setDeleted(rs.getBoolean("deleted"));
            return user;
        };
    }

    /**
     * 把账号分页查询结果映射成后台用户列表视图。
     * @return 账号表聚合数据，键名与前端展示字段保持一致。
     */
    private RowMapper<UserPageVo> userPageRowMapper() {
        return (rs, rowNum) -> {
            UserPageVo vo = new UserPageVo();
            vo.setId(rs.getInt("id"));
            vo.setUsername(rs.getString("username"));
            vo.setEnable(rs.getBoolean("enable_flag"));
            vo.setNickname(rs.getString("nickname"));
            vo.setPhone(rs.getString("phone"));
            vo.setAvatar(rs.getString("avatar"));
            vo.setEx1(rs.getObject("ex1") == null ? 0 : rs.getInt("ex1"));
            vo.setRoleId(rs.getLong("role_id"));
            vo.setRoleName(rs.getString("role_name"));
            Timestamp createdTime = rs.getTimestamp("created_time");
            if (createdTime != null) {
                vo.setCreatedTime(createdTime.toLocalDateTime());
            }
            Timestamp lastLogin = rs.getTimestamp("last_login");
            if (lastLogin != null) {
                vo.setLastLogin(lastLogin.toLocalDateTime());
            }
            return vo;
        };
    }

    /**
     * 转换账号表字段格式，便于后续计算或接口返回。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 账号表在该步骤产出的业务结果。
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }
}
