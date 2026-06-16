package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.Notification;
import com.sys.pro.service.NotificationService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NotificationController 接收前端请求、校验入口参数并调用业务服务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;
    /**
     * 读取当前用户通知列表，展示举报、艾特和审核处理进度。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */

    @ApiOperation(value = "查询我的通知")
    @GetMapping("/my")
    public CommonResult<List<Notification>> listMyNotifications(@RequestParam(defaultValue = "30") Integer limit) {
        return CommonResult.success(notificationService.listRecent(getUserId(), limit));
    }

    /**
     * 统计当前用户未读通知数量，用于顶部角标提醒。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "查询我的未读通知数")
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(notificationService.unreadCount(getUserId()));
    }

    /**
     * 将指定通知标记为已读，避免重复提醒用户。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "标记通知已读")
    @PostMapping("/read/{id}")
    public CommonResult<Void> markRead(@PathVariable("id") Long id) {
        notificationService.markRead(id, getUserId());
        return CommonResult.success();
    }
}
