package com.sys.pro.vo;

import lombok.Data;

/**
 * LoginParam 封装Login页面响应数据，按前端展示需要组织字段。
 */
@Data
public class LoginParam {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
