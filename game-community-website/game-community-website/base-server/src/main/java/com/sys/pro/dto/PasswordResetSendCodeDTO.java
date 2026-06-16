package com.sys.pro.dto;

import lombok.Data;

/**
 * 找回密码验证码发送请求。
 * 用户需要同时填写账号标识和已绑定邮箱，后台校验匹配后才会发送验证码。
 */
@Data
public class PasswordResetSendCodeDTO {

    /**
     * 用户名、手机号或邮箱，用来定位需要重置密码的账号。
     */
    private String identifier;

    /**
     * 账号资料中已绑定的邮箱，用来接收一次性验证码。
     */
    private String email;
}
