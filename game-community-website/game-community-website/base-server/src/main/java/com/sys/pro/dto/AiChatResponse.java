package com.sys.pro.dto;

import com.sys.pro.pojo.AiConversation;
import com.sys.pro.pojo.AiMessage;
import lombok.Data;

import java.util.List;

/**
 * AiChatResponse 封装AiChatResponse请求参数，避免控制器直接暴露数据库实体。
 */
@Data
public class AiChatResponse {
    private AiConversation conversation;
    private List<AiMessage> messages;
    private String answer;
}
