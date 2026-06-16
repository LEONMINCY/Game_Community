package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.MenuMapper;
import com.sys.pro.pojo.Menu;
import com.sys.pro.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MenuServiceImpl 承接后台菜单核心业务规则，协调数据访问、缓存、通知和外部服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {



    private final MenuMapper menuMapper;



    /**
     * 完成后台菜单中的 selectByRoleId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param roleId role 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 后台菜单列表数据。
     */
    @Override
    public List<Menu> selectByRoleId(Integer roleId, Integer userId) {
        return menuMapper.selectByRoleId(roleId);
    }

}
