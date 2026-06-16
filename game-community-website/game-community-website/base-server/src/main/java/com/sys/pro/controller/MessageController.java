package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.pojo.Message;
import com.sys.pro.service.MessageApplicationService;
import com.sys.pro.service.MessageService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MessageController 接收前端请求、校验入口参数并调用业务服务。
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/message")
public class MessageController extends BaseController {

    private final MessageService messageService;
    private final MessageApplicationService messageApplicationService;

    /**
     * 根据主键读取站内私信详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "根据id查询消息")
    @GetMapping("/get/{id}")
    public CommonResult<Message> getById(@PathVariable("id") Long id) {
        return CommonResult.success(messageService.getById(id));
    }

    /**
     * 接收新增站内私信数据，完成入口校验后交由业务层保存。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "发送消息")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody Message message) {
        messageApplicationService.sendMessage(message, getUserId());
        return CommonResult.success();
    }

    /**
     * 按主键删除站内私信记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除消息")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        messageService.deleteMessage(id);
        return CommonResult.success();
    }

    /**
     * 保存站内私信编辑后的内容，让前台展示和后台管理保持一致。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新消息")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody Message message) {
        messageService.updateMessage(message);
        return CommonResult.success();
    }

    /**
     * 读取站内私信列表数据，按页面传入条件完成筛选和排序。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "查询消息列表")
    @GetMapping("/list/{id}")
    public CommonResult<List<Message>> list(@PathVariable("id") Integer id) {
        return CommonResult.success(messageService.listConversation(getUserId(), id));
    }

    /**
     * 读取站内私信的 ChatList 数据，供页面展示或后续业务判断。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "获取聊天列表")
    @GetMapping("/chat-list")
    public CommonResult<List<Map<String, Object>>> getChatList() {
        return CommonResult.success(messageService.chatList(getUserId()));
    }

    /**
     * 统计当前用户未读私信总数，供首页消息中心旁的红点角标使用。
     * @return 包装后的接口响应，包含未读私信数量。
     */
    @ApiOperation(value = "查询未读私信数量")
    @GetMapping("/unread-count")
    public CommonResult<Long> unreadCount() {
        return CommonResult.success(messageService.unreadCount(getUserId()));
    }

    /**
     * 打开会话后将该联系人发给当前用户的消息标记为已读，避免重复提醒。
     * @param otherUserId 会话另一方用户主键。
     * @return 包装后的接口响应，表示已读状态已同步。
     */
    @ApiOperation(value = "标记会话已读")
    @PostMapping("/read/{otherUserId}")
    public CommonResult<Void> markConversationRead(@PathVariable("otherUserId") Integer otherUserId) {
        messageService.markConversationRead(getUserId(), otherUserId);
        return CommonResult.success();
    }
}
