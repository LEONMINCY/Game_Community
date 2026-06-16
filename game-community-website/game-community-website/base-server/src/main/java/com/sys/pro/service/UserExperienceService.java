package com.sys.pro.service;

import com.sys.pro.repository.UserExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * 用户经验和每日任务业务服务。
 * 负责每日上限判断与任务展示，数据库访问交给 UserExperienceRepository。
 */
@Service
@RequiredArgsConstructor
public class UserExperienceService {

    private static final int DAILY_LIMIT = 120;

    private final UserExperienceRepository userExperienceRepository;

    /**
     * 完成UserExperience中的 award 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param taskType taskType 字段，来源于当前接口入参或内部调用上下文。
     * @param expPerAction expPerAction 字段，来源于当前接口入参或内部调用上下文。
     * @param maxCount maxCount 字段，来源于当前接口入参或内部调用上下文。
     * @param maxExp maxExp 字段，来源于当前接口入参或内部调用上下文。
     */
    public void award(Integer userId, String taskType, int expPerAction, int maxCount, int maxExp) {
        if (userId == null || taskType == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        Integer currentCount = userExperienceRepository.taskActionCount(userId, today, taskType);
        Integer currentExp = userExperienceRepository.taskExperience(userId, today, taskType);
        Integer totalExpToday = userExperienceRepository.dailyExperience(userId, today);

        if (currentCount >= maxCount || currentExp >= maxExp || totalExpToday >= DAILY_LIMIT) {
            return;
        }

        int addExp = Math.min(expPerAction, Math.min(maxExp - currentExp, DAILY_LIMIT - totalExpToday));
        if (addExp <= 0) {
            return;
        }

        userExperienceRepository.addTaskExperience(userId, today, taskType, addExp);
        userExperienceRepository.addUserExperience(userId, addExp);
    }

    /**
     * 汇总当前用户每日任务进度和可获得经验。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return UserExperience聚合数据，键名与前端展示字段保持一致。
     */
    public Map<String, Object> summary(Integer userId) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = userExperienceRepository.listDailyTasks(userId, today);
        Map<String, Map<String, Object>> byType = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byType.put(String.valueOf(row.get("task_type")), row);
        }

        List<Map<String, Object>> tasks = new ArrayList<>();
        tasks.add(task("POST", "发布帖子", "今天最多 3 篇，每篇 +20 经验", 3, 60, byType));
        tasks.add(task("COMMENT", "发布评论", "今天最多 10 条，每条 +5 经验", 10, 50, byType));
        tasks.add(task("PURCHASE", "购买游戏", "今天最多 2 款，每款 +30 经验", 2, 60, byType));

        int totalExp = rows.stream().mapToInt(row -> ((Number) row.getOrDefault("exp_gained", 0)).intValue()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", today.toString());
        result.put("dailyLimit", DAILY_LIMIT);
        result.put("totalExp", totalExp);
        result.put("tasks", tasks);
        return result;
    }

    /**
     * 完成UserExperience中的 task 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param desc desc 字段，来源于当前接口入参或内部调用上下文。
     * @param maxCount maxCount 字段，来源于当前接口入参或内部调用上下文。
     * @param maxExp maxExp 字段，来源于当前接口入参或内部调用上下文。
     * @param byType byType 字段，来源于当前接口入参或内部调用上下文。
     * @return UserExperience聚合数据，键名与前端展示字段保持一致。
     */
    private Map<String, Object> task(String type, String title, String desc, int maxCount, int maxExp, Map<String, Map<String, Object>> byType) {
        Map<String, Object> row = byType.getOrDefault(type, Collections.emptyMap());
        int count = ((Number) row.getOrDefault("action_count", 0)).intValue();
        int exp = ((Number) row.getOrDefault("exp_gained", 0)).intValue();
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("type", type);
        task.put("title", title);
        task.put("desc", desc);
        task.put("count", count);
        task.put("maxCount", maxCount);
        task.put("exp", exp);
        task.put("maxExp", maxExp);
        task.put("done", count >= maxCount || exp >= maxExp);
        return task;
    }

}
