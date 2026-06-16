package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.UserBlacklist;
import com.sys.pro.service.UserBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 黑名单接口，提供拉黑、取消拉黑和黑名单查询能力。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/blacklist")
public class UserBlacklistController extends BaseController {

    private final UserBlacklistService userBlacklistService;

    /**
     * 读取用户黑名单列表数据，按页面传入条件完成筛选和排序。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @GetMapping("/list")
    public CommonResult<List<UserBlacklist>> list() {
        return CommonResult.success(userBlacklistService.listByUser(getUserId()));
    }

    /**
     * 把指定用户加入黑名单，阻止对方继续发送私信。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/block/{blockedUserId}")
    public CommonResult<Void> block(@PathVariable Integer blockedUserId) {
        userBlacklistService.block(getUserId(), blockedUserId);
        return CommonResult.success();
    }

    /**
     * 从黑名单中移除指定用户，恢复双方私信能力。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @DeleteMapping("/unblock/{blockedUserId}")
    public CommonResult<Void> unblock(@PathVariable Integer blockedUserId) {
        userBlacklistService.unblock(getUserId(), blockedUserId);
        return CommonResult.success();
    }
}
