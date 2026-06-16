package com.sys.pro.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * UserParam 封装用户账号页面响应数据，按前端展示需要组织字段。
 */
@Setter
@Getter
@ToString(callSuper = true)
public class UserParam {

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 角色id
     */
    private Integer roleId;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
