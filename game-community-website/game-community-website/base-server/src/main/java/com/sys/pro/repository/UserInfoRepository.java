package com.sys.pro.repository;

import com.sys.pro.pojo.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户资料复杂查询仓储。
 * 跨三张账号表的联合搜索放在持久化层，Service 只负责搜索规则和展示字段装饰。
 */
@Repository
@RequiredArgsConstructor
public class UserInfoRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 从三类账号表联合检索用户，并补齐粉丝和内容数量。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 用户资料列表数据。
     */
    public List<UserInfo> searchUsersFromAccountTables(String keyword, int limit, Integer currentUserId) {
        boolean hasKeyword = keyword != null && !keyword.isEmpty();
        String followSelect = currentUserId == null ? "0 AS followed, "
                : "EXISTS(SELECT 1 FROM t_user_relation rel2 WHERE rel2.user_id = ? "
                + "AND rel2.follow_id = account.id AND COALESCE(rel2.deleted, 0) = 0) AS followed, ";
        String sql = "SELECT COALESCE(ui.id, account.id) AS id, "
                + "account.id AS user_id, "
                + "COALESCE(NULLIF(ui.nickname, ''), account.username) AS nickname, "
                + "ui.avatar, ui.ex1, ui.ex2, COALESCE(ui.deleted, 0) AS deleted, "
                + "account.role_id, account.username, "
                + followSelect
                + "(SELECT COUNT(*) FROM t_user_relation rel WHERE rel.follow_id = account.id AND COALESCE(rel.deleted, 0) = 0) AS fans_count, "
                + "(SELECT COUNT(*) FROM t_post p WHERE p.user_id = account.id AND COALESCE(p.deleted, 0) = 0 "
                + "AND (p.audit_status = 'approved' OR p.audit_status IS NULL OR p.audit_status = '')) AS post_count "
                + "FROM ("
                + "SELECT id, username, role_id FROM t_player_user WHERE deleted = 0 "
                + "UNION ALL SELECT id, username, role_id FROM t_moderator_user WHERE deleted = 0 "
                + "UNION ALL SELECT id, username, role_id FROM t_admin_user WHERE deleted = 0"
                + ") account "
                + "LEFT JOIN t_user_info ui ON ui.user_id = account.id "
                + "WHERE COALESCE(ui.deleted, 0) = 0 "
                + (hasKeyword ? "AND (account.username LIKE ? OR ui.nickname LIKE ?) " : "")
                + "ORDER BY COALESCE(ui.ex1, 0) DESC, account.id DESC LIMIT ?";

        List<Object> args = new ArrayList<>();
        if (currentUserId != null) {
            args.add(currentUserId);
        }
        if (hasKeyword) {
            String likeKeyword = "%" + keyword + "%";
            args.add(likeKeyword);
            args.add(likeKeyword);
        }
        args.add(limit);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(rs.getInt("id"));
            userInfo.setUserId(rs.getInt("user_id"));
            userInfo.setNickname(rs.getString("nickname"));
            userInfo.setAvatar(rs.getString("avatar"));
            userInfo.setEx1(readInteger(rs.getObject("ex1")));
            userInfo.setEx2(readInteger(rs.getObject("ex2")));
            userInfo.setDeleted(rs.getBoolean("deleted"));
            userInfo.setRoleId(readInteger(rs.getObject("role_id")));
            userInfo.setUsername(rs.getString("username"));
            userInfo.setFollow(rs.getBoolean("followed"));
            userInfo.setFansCount(readInteger(rs.getObject("fans_count")));
            userInfo.setPostCount(readInteger(rs.getObject("post_count")));
            return userInfo;
        }, args.toArray());
    }

    /**
     * 把数据库返回的数字字段安全转换成整数。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 用户资料统计值或主键结果。
     */
    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
