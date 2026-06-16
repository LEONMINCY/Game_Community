package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PublicUserSearchController 接收前端请求、校验入口参数并调用业务服务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/noLogin/user-info")
public class PublicUserSearchController extends BaseController {

    private final UserInfoService userInfoService;
    /**
     * 按用户名、昵称或手机号检索用户，供全局搜索和关注选择使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    @GetMapping("/search")
    public CommonResult<List<UserInfo>> searchUsers(@RequestParam(value = "keyword", required = false) String keyword,
                                                    @RequestParam(defaultValue = "10") Integer limit) {
        return CommonResult.success(userInfoService.searchUsers(keyword, limit, getUserId()));
    }
}
