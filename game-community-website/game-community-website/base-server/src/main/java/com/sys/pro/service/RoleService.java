package com.sys.pro.service;

import com.sys.pro.pojo.Role;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * RoleService 定义角色权限业务能力，供控制器和其他服务组合调用。
 */
public interface RoleService extends IService<Role> {

    /**
     * 完成角色权限中的 createRole 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param role role 字段，来源于当前接口入参或内部调用上下文。
     */
    void createRole(Role role);

    /**
     * 完成角色权限中的 deleteRole 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteRole(Long id);

    /**
     * 完成角色权限中的 updateRole 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param role role 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateRole(Role role);
}
