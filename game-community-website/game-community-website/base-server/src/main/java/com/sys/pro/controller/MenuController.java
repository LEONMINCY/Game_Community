package com.sys.pro.controller;

import com.sys.pro.service.MenuService;
import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.Menu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.sys.pro.common.CommonResult.success;

/**
 * MenuController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/menu")
public class MenuController extends BaseController {



    private final MenuService menuService;



    /**
     * 读取后台菜单的 List 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/list")
    public CommonResult<List<Menu>> getList() {
        final Integer userId = getUserId();
        final Integer roleId = getRoleId();
        log.info("查询用户菜单列表，参数：{},{}", roleId, userId);
        List<Menu> menuList = menuService.selectByRoleId(roleId, userId);
        return success(menuList);
    }

}
