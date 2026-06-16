package com.sys.pro.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * UserInfoVo 封装用户资料页面响应数据，按前端展示需要组织字段。
 */
@Data
@ApiModel(value="UserInfoVo", description="用户信息展示对象")
public class UserInfoVo {

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "头像")
    private String avatar;

    @ApiModelProperty(value = "等级")
    private Integer level;

    @ApiModelProperty(value = "是否已关注")
    private Boolean isFollowed;
} 