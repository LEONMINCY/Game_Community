package com.sys.pro.dto;

import lombok.Data;

/**
 * 找回密码重置请求。
 * 验证码、账号和邮箱必须同时匹配，才允许写入新的密码哈希。
 */
@Data
public class PasswordResetDTO {

    /**
     * 用户名、手机号或邮箱，用来定位需要重置密码的账号。
     */
    private String identifier;

    /**
     * 账号资料中已绑定的邮箱。
     */
    private String email;

    /**
     * 邮件验证码，默认十分钟内有效。
     */
    private String code;

    /**
     * 用户设置的新密码。
     */
    private String newPassword;

    /**
     * 新密码二次确认值，防止前端输入错误。
     */
    private String confirmPassword;
}
