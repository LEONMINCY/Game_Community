package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.dto.PasswordResetDTO;
import com.sys.pro.dto.PasswordResetSendCodeDTO;
import com.sys.pro.service.PasswordResetService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 找回密码控制器。
 * 只负责接收前端提交的邮箱验证码请求和密码重置请求，具体校验与发送逻辑交给 Service。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/noLogin/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * 发送邮箱验证码，用于忘记密码场景。
     * @param request 前端提交的账号标识和绑定邮箱。
     * @return 统一接口响应。
     */
    @ApiOperation(value = "发送找回密码邮箱验证码")
    @PostMapping("/send-code")
    public CommonResult<Void> sendCode(@RequestBody PasswordResetSendCodeDTO request) {
        passwordResetService.sendCode(request);
        return CommonResult.success();
    }

    /**
     * 根据邮箱验证码重置密码。
     * @param request 前端提交的账号、邮箱、验证码和新密码。
     * @return 统一接口响应。
     */
    @ApiOperation(value = "邮箱验证码重置密码")
    @PostMapping("/reset")
    public CommonResult<Void> resetPassword(@RequestBody PasswordResetDTO request) {
        passwordResetService.resetPassword(request);
        return CommonResult.success();
    }
}
