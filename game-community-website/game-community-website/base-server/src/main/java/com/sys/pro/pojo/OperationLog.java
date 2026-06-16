package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OperationLog 是操作日志实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private String username;

    private Integer roleId;

    private String roleName;

    private String moduleName;

    private String operationName;

    private String controllerName;

    private String methodName;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String ipAddress;

    private Boolean success;

    private String errorMessage;

    private Long costTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    private Boolean deleted;
}
