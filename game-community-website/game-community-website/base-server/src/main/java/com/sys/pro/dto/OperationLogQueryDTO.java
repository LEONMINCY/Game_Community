package com.sys.pro.dto;

import com.sys.pro.common.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OperationLogQueryDTO 封装操作日志请求参数，避免控制器直接暴露数据库实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQueryDTO extends PageParam {

    private String username;

    private Integer roleId;

    private Boolean success;

    private String keyword;

    private String startTime;

    private String endTime;
}
