package com.sys.pro.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.pojo.Menu;

import java.util.List;

/**
 * MenuService 定义后台菜单业务能力，供控制器和其他服务组合调用。
 */
public interface MenuService extends IService<Menu> {



    /**
     * 完成后台菜单中的 selectByRoleId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param roleId role 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 后台菜单列表数据。
     */

    List<Menu> selectByRoleId(Integer roleId, Integer userId);



}

