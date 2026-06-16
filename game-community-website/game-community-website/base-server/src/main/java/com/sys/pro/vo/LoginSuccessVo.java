package com.sys.pro.vo;

import lombok.Data;

/**
 * LoginSuccessVo 封装LoginSuccess页面响应数据，按前端展示需要组织字段。
 */
@Data
public class LoginSuccessVo {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 用户名
     */
    private String username;

    /**
     * token
     */
    private String token;
}
