package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 旧账号表实体，对应 t_user；新账号按角色拆分后该表仅用于兼容历史数据。
 */
@Data
@TableName("t_user")
public class LegacyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 登录账号。
     */
    private String username;

    /**
     * 加密后的登录密码。
     */
    private String password;

    /**
     * 角色ID。
     */
    private Integer roleId;

    /**
     * 账号是否启用。
     */
    private Boolean enableFlag;

    /**
     * 禁封截止时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime banEndTime;

    /**
     * 最近登录时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLogin;

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

    /**
     * 逻辑删除标记。
     */
    private Boolean deleted;
}
