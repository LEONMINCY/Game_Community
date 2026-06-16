package com.sys.pro.dto;

import com.sys.pro.common.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * UserPageDTO 封装用户账号请求参数，避免控制器直接暴露数据库实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "用户分页查询参数")
public class UserPageDTO extends PageParam {
    
    @ApiModelProperty(value = "用户昵称")
    private String nickname;
    
    @ApiModelProperty(value = "手机号")
    private String phone;

    @ApiModelProperty(value = "角色ID")
    private Integer roleId;
    
    @ApiModelProperty(value = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    
    @ApiModelProperty(value = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
} 
