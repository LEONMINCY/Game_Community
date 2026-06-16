package com.sys.pro.common;

/**
 * GlobalErrorCodeConstants 提供GlobalErrorCodeConstants通用能力，统一项目内重复使用的基础模型或拦截逻辑。
 */
public interface GlobalErrorCodeConstants {
    ErrorCode SUCCESS = new ErrorCode(200, "成功");

    // ========== 客户端错误段 ==========

    ErrorCode BAD_REQUEST = new ErrorCode(400, "请求参数不正确");
    ErrorCode UNAUTHORIZED = new ErrorCode(401, "账号未登录");
    ErrorCode FORBIDDEN = new ErrorCode(403, "没有该操作权限");
    ErrorCode NOT_FOUND = new ErrorCode(404, "请求未找到");
    ErrorCode METHOD_NOT_ALLOWED = new ErrorCode(405, "请求方法不正确");
    // 并发请求，不允许
    ErrorCode LOCKED = new ErrorCode(423, "请求失败，请稍后重试");
    ErrorCode TOO_MANY_REQUESTS = new ErrorCode(429, "请求过于频繁，请稍后重试");

    // ========== 服务端错误段 ==========

    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(500, "系统异常");
    ErrorCode NOT_IMPLEMENTED = new ErrorCode(501, "功能未实现/未开启");

    // ========== 自定义错误段 ==========
    // 重复请求
    ErrorCode REPEATED_REQUESTS = new ErrorCode(600, "重复请求，请稍后重试");
    ErrorCode LOGIN_FAIL = new ErrorCode(601, "账号或密码错误");
    ErrorCode USERNAME_EXIST  = new ErrorCode(602,"用户名已存在");
    ErrorCode PASSWORD_ERROR  = new ErrorCode(603,"旧密码与原始密码不符合");
    ErrorCode FORBIDDEN_LOGIN  = new ErrorCode(604,"账号已经禁止登录，请联系管理员");
    ErrorCode USER_NOT_EXIST  = new ErrorCode(605,"该账号不存在");
    ErrorCode PHONE_EXIST  = new ErrorCode(606,"手机号已被注册");
    ErrorCode INFO_DEFICIENCY  = new ErrorCode(607,"信息缺失");



    ErrorCode UNKNOWN = new ErrorCode(699, "未知错误");

    /**
     * 是否为服务端错误，参考 HTTP 5XX 错误码段
     *
     * @param code 错误码
     * @return 是否
     */
    static boolean isServerErrorCode(Integer code) {
        return code != null
                && code >= INTERNAL_SERVER_ERROR.getCode() && code <= INTERNAL_SERVER_ERROR.getCode() + 99;
    }
}
