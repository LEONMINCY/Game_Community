package com.sys.pro.controller.common;

import com.sys.pro.pojo.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 控制器基类，统一读取当前登录用户并提供角色判断辅助方法。
 */
public abstract class BaseController {

    /**
     * 读取Base的 UserId 数据，供页面展示或后续业务判断。
     * @return Base统计值或主键结果。
     */
    public final Integer getUserId() {
        final UserInfo user = getUserInfo();
        return user.getUserId();
    }

    /**
     * 读取Base的 RoleId 数据，供页面展示或后续业务判断。
     * @return Base统计值或主键结果。
     */
    public final Integer getRoleId() {
        final UserInfo user = getUserInfo();
        return user.getRoleId();
    }

    /**
     * 读取Base的 UserName 数据，供页面展示或后续业务判断。
     * @return Base处理后的文本结果。
     */
    public final String getUserName() {
        final UserInfo user = getUserInfo();
        return user.getUsername();
    }

    /**
     * 读取Base的 UserInfo 数据，供页面展示或后续业务判断。
     * @return Base在该步骤产出的业务结果。
     */
    public UserInfo getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserInfo)) {
            return new UserInfo();
        }
        return (UserInfo) authentication.getPrincipal();
    }

}
