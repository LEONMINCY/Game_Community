package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * UserBlacklist 是用户黑名单实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_user_blacklist")
public class UserBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private Integer blockedUserId;

    private LocalDateTime createTime;

    private Boolean deleted;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatar;
}
