package com.sys.pro.service.impl;

import com.sys.pro.pojo.Role;
import com.sys.pro.service.RoleService;
import com.sys.pro.mapper.RoleMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现，查询后台角色和菜单权限。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    /**
     * 完成角色权限中的 createRole 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param role role 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createRole(Role role) {
        save(role);
    }

    /**
     * 完成角色权限中的 deleteRole 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteRole(Long id) {
        removeById(id);
    }

    /**
     * 完成角色权限中的 updateRole 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param role role 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateRole(Role role) {
        updateById(role);
    }
}
