package com.sys.pro.service;

import com.sys.pro.dto.AiChatRequest;
import com.sys.pro.dto.AiChatResponse;
import com.sys.pro.dto.AiConversationRequest;
import com.sys.pro.pojo.AiConversation;
import com.sys.pro.pojo.AiMessage;

import java.util.List;

/**
 * AI 助手应用服务。
 */
public interface AiAssistantApplicationService {
    /**
     * 读取 AI 助手历史会话，恢复用户之前的问答上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */

    List<AiConversation> conversations(Integer userId);
    /**
     * 创建 AI 助手会话，保存用户与模型的上下文边界。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return AI 助手在该步骤产出的业务结果。
     */

    AiConversation createConversation(Integer userId, AiConversationRequest request);
    /**
     * 读取指定 AI 会话的消息记录。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */

    List<AiMessage> messages(Long conversationId, Integer userId);
    /**
     * 清理指定时间范围内的数据记录，减少后台列表干扰。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手列表数据。
     */

    List<AiMessage> clear(Long conversationId, Integer userId);
    /**
     * 按主键删除AI 助手记录，并让业务层同步处理关联状态。
     * @param conversationId conversation 主键，用来定位关联业务数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void delete(Long conversationId, Integer userId);
    /**
     * 接收用户发给 AI 助手的消息，并返回模型回复结果。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return AI 助手在该步骤产出的业务结果。
     */

    AiChatResponse chat(AiChatRequest request, Integer userId);
}
