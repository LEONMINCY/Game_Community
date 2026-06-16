package com.sys.pro.pojo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Unified account DTO used by controllers and services.
 * Real persistence entities are PlayerUser, ModeratorUser and AdminUser.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "User", description = "Unified account DTO")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "username")
    private String username;

    @ApiModelProperty(value = "password")
    private String password;

    @ApiModelProperty(value = "role id")
    private Long roleId;

    @ApiModelProperty(value = "enabled")
    private Boolean enableFlag;

    @ApiModelProperty(value = "ban end time")
    private LocalDateTime banEndTime;

    @ApiModelProperty(value = "last login")
    private LocalDateTime lastLogin;

    @ApiModelProperty(value = "created time")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "updated time")
    private LocalDateTime updatedTime;

    @ApiModelProperty(value = "deleted")
    private Boolean deleted;

    private String phone;

    private String email;

    private String gender;

    private Integer age;

    private String name;
}
