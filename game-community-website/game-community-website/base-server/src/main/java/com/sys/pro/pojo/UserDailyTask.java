package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日任务实体，对应 t_user_daily_task。
 */
@Data
@TableName("t_user_daily_task")
public class UserDailyTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 每日任务记录主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID。
     */
    private Integer userId;

    /**
     * 任务日期。
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate taskDate;

    /**
     * 任务类型，例如发帖、评论或购买游戏。
     */
    private String taskType;

    /**
     * 当日完成次数。
     */
    private Integer actionCount;

    /**
     * 当日获得经验值。
     */
    private Integer expGained;

    /**
     * 创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 更新时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;
}
