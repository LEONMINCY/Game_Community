package com.sys.pro.service;

import com.sys.pro.dto.PasswordResetDTO;
import com.sys.pro.dto.PasswordResetSendCodeDTO;

/**
 * 找回密码业务接口。
 * 负责发送邮箱验证码、校验验证码并重置账号密码。
 */
public interface PasswordResetService {

    /**
     * 校验账号和邮箱后发送一次性验证码。
     * @param request 前端提交的账号标识和绑定邮箱。
     */
    void sendCode(PasswordResetSendCodeDTO request);

    /**
     * 校验邮箱验证码并写入新的密码哈希。
     * @param request 前端提交的账号、邮箱、验证码和新密码。
     */
    void resetPassword(PasswordResetDTO request);
}
