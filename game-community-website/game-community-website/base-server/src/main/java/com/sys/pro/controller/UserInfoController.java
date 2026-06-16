package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.UserInfoService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.sys.pro.common.CommonResult.success;

/**
 * UserInfoController 接收前端请求、校验入口参数并调用业务服务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user-info")
public class UserInfoController extends BaseController {

    private final UserInfoService userInfoService;

    /**
     * 读取用户资料的 Info 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "查询当前登录用户资料")
    @GetMapping("/get")
    public CommonResult<UserInfo> getInfo() {
        return success(this.getUserInfo());
    }

    /**
     * 读取用户资料的 RealInfo 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "查询指定用户主页资料")
    @GetMapping("/get-real/{id}")
    public CommonResult<UserInfo> getRealInfo(@PathVariable Integer id) {
        return success(userInfoService.getProfileView(this.getUserId(), id));
    }

    /**
     * 读取用户资料的 RealInfo 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "查询当前登录用户个人中心资料")
    @GetMapping("/get-real")
    public CommonResult<UserInfo> getRealInfo() {
        return success(userInfoService.getOwnProfileView(this.getUserId()));
    }

    /**
     * 按用户名、昵称或手机号检索用户，供全局搜索和关注选择使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "搜索用户公开信息")
    @GetMapping("/search")
    public CommonResult<List<UserInfo>> searchUsers(@RequestParam(value = "keyword", required = false) String keyword,
                                                    @RequestParam(defaultValue = "10") Integer limit) {
        return success(userInfoService.searchUsers(keyword, limit, getUserId()));
    }

    /**
     * 保存用户资料编辑后的内容，让前台展示和后台管理保持一致。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "修改当前登录用户资料")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody UserInfo userInfo) {
        userInfoService.updateProfile(this.getUserId(), userInfo);
        return success();
    }

    /**
     * 完成用户每日签到，增加经验并刷新等级进度。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "每日签到")
    @PutMapping("/clock")
    public CommonResult<Void> clock() {
        userInfoService.clock(this.getUserId());
        return success();
    }
}
