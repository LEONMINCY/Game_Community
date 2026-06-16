package com.sys.pro.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户每日任务和经验值持久化仓储。
 */
@Repository
@RequiredArgsConstructor
public class UserExperienceRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 统计用户当天某类任务完成次数。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param date date 字段，来源于当前接口入参或内部调用上下文。
     * @param taskType taskType 字段，来源于当前接口入参或内部调用上下文。
     * @return UserExperience统计值或主键结果。
     */
    public int taskActionCount(Integer userId, LocalDate date, String taskType) {
        return queryInt("SELECT COALESCE(action_count, 0) FROM t_user_daily_task WHERE user_id = ? AND task_date = ? AND task_type = ?",
                userId, date, taskType);
    }

    /**
     * 统计用户当天某类任务已获得经验。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param date date 字段，来源于当前接口入参或内部调用上下文。
     * @param taskType taskType 字段，来源于当前接口入参或内部调用上下文。
     * @return UserExperience统计值或主键结果。
     */
    public int taskExperience(Integer userId, LocalDate date, String taskType) {
        return queryInt("SELECT COALESCE(exp_gained, 0) FROM t_user_daily_task WHERE user_id = ? AND task_date = ? AND task_type = ?",
                userId, date, taskType);
    }

    /**
     * 统计用户当天累计获得的任务经验。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param date date 字段，来源于当前接口入参或内部调用上下文。
     * @return UserExperience统计值或主键结果。
     */
    public int dailyExperience(Integer userId, LocalDate date) {
        return queryInt("SELECT COALESCE(SUM(exp_gained), 0) FROM t_user_daily_task WHERE user_id = ? AND task_date = ?",
                userId, date);
    }

    /**
     * 写入每日任务经验明细，限制每日经验上限。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param date date 字段，来源于当前接口入参或内部调用上下文。
     * @param taskType taskType 字段，来源于当前接口入参或内部调用上下文。
     * @param addExp addExp 字段，来源于当前接口入参或内部调用上下文。
     */
    public void addTaskExperience(Integer userId, LocalDate date, String taskType, int addExp) {
        jdbcTemplate.update("INSERT INTO t_user_daily_task (user_id, task_date, task_type, action_count, exp_gained, created_time, updated_time) " +
                        "VALUES (?, ?, ?, 1, ?, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE action_count = action_count + 1, exp_gained = exp_gained + VALUES(exp_gained), updated_time = NOW()",
                userId, date, taskType, addExp);
    }

    /**
     * 累加用户社区经验，并触发等级进度更新。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param addExp addExp 字段，来源于当前接口入参或内部调用上下文。
     */
    public void addUserExperience(Integer userId, int addExp) {
        jdbcTemplate.update("UPDATE t_user_info SET ex1 = COALESCE(ex1, 0) + ? WHERE user_id = ?", addExp, userId);
    }

    /**
     * 汇总UserExperience列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param date date 字段，来源于当前接口入参或内部调用上下文。
     * @return UserExperience列表数据。
     */
    public List<Map<String, Object>> listDailyTasks(Integer userId, LocalDate date) {
        return jdbcTemplate.queryForList(
                "SELECT task_type, action_count, exp_gained FROM t_user_daily_task WHERE user_id = ? AND task_date = ?",
                userId,
                date
        );
    }

    /**
     * 执行单值统计查询，并把空结果安全转换为整数。
     * @param sql sql 字段，来源于当前接口入参或内部调用上下文。
     * @param args 启动参数，当前项目通常不直接使用。
     * @return UserExperience统计值或主键结果。
     */
    private Integer queryInt(String sql, Object... args) {
        List<Integer> list = jdbcTemplate.queryForList(sql, Integer.class, args);
        return list.isEmpty() || list.get(0) == null ? 0 : list.get(0);
    }
}
