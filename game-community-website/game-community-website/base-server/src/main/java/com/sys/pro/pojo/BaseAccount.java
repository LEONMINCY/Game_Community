package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Common fields shared by the three physical account tables.
 */
@Data
public abstract class BaseAccount implements Serializable {

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private String username;

    private String password;

    private Long roleId;

    private Boolean enableFlag;

    private LocalDateTime banEndTime;

    private LocalDateTime lastLogin;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private Boolean deleted;
}
