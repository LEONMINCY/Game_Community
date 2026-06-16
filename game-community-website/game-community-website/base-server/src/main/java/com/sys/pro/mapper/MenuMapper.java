package com.sys.pro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sys.pro.pojo.Menu;

import java.util.List;

/**
 * MenuMapper 是后台菜单的数据访问入口，负责MyBatis-Plus基础读写和扩展查询。
 */
public interface MenuMapper extends BaseMapper<Menu> {
    /**
     * 完成后台菜单中的 selectByRoleId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param roleId role 主键，用来定位关联业务数据。
     * @return 后台菜单列表数据。
     */



    List<Menu> selectByRoleId(Integer roleId);



}

