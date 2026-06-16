package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.dto.AiChatRequest;
import com.sys.pro.dto.AiChatResponse;
import com.sys.pro.dto.AiConversationRequest;
import com.sys.pro.pojo.AiConversation;
import com.sys.pro.pojo.AiMessage;
import com.sys.pro.service.AiAssistantApplicationService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 助手接口层。
 * 会话管理、消息保存和模型调用由应用服务处理。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai-assistant")
public class AiAssistantController extends BaseController {

    private final AiAssistantApplicationService aiAssistantApplicationService;

    /**
     * 读取 AI 助手历史会话，恢复用户之前的问答上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "AI会话列表")
    @GetMapping("/conversations")
    public CommonResult<List<AiConversation>> conversations() {
        return CommonResult.success(aiAssistantApplicationService.conversations(getUserId()));
    }

    /**
     * 新建一条 AI 助手会话，供前端切换到空白对话窗口。
     * @param request 会话标题等创建参数。
     * @return 包装后的接口响应，包含新建后的会话信息。
     */
    @ApiOperation(value = "创建AI会话")
    @PostMapping("/conversations")
    public CommonResult<AiConversation> createConversation(@RequestBody AiConversationRequest request) {
        return CommonResult.success(aiAssistantApplicationService.createConversation(getUserId(), request));
    }

    /**
     * 读取指定 AI 会话下的全部消息，刷新页面或切换历史对话时使用。
     * @param id AI 会话主键。
     * @return 包装后的接口响应，包含该会话的消息列表。
     */
    @ApiOperation(value = "AI会话消息列表")
    @GetMapping("/conversations/{id}/messages")
    public CommonResult<List<AiMessage>> messages(@PathVariable("id") Long id) {
        return CommonResult.success(aiAssistantApplicationService.messages(id, getUserId()));
    }

    /**
     * 清空指定 AI 会话的消息记录，但保留会话本身方便继续提问。
     * @param id AI 会话主键。
     * @return 包装后的接口响应，返回清空后的消息列表。
     */
    @ApiOperation(value = "清空AI会话消息")
    @PostMapping("/conversations/{id}/clear")
    public CommonResult<List<AiMessage>> clear(@PathVariable("id") Long id) {
        return CommonResult.success(aiAssistantApplicationService.clear(id, getUserId()));
    }

    /**
     * 删除指定 AI 会话，历史列表不再展示该对话。
     * @param id AI 会话主键。
     * @return 包装后的接口响应，表示删除操作完成。
     */
    @ApiOperation(value = "删除AI会话")
    @DeleteMapping("/conversations/{id}")
    public CommonResult<Boolean> delete(@PathVariable("id") Long id) {
        aiAssistantApplicationService.delete(id, getUserId());
        return CommonResult.success(true);
    }

    /**
     * 接收用户发给 AI 助手的消息，并返回模型回复结果。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "发送AI消息")
    @PostMapping("/chat")
    public CommonResult<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return CommonResult.success(aiAssistantApplicationService.chat(request, getUserId()));
    }
}
