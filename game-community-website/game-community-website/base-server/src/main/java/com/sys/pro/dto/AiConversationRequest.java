package com.sys.pro.dto;

import lombok.Data;

/**
 * AiConversationRequest 封装AI会话请求参数，避免控制器直接暴露数据库实体。
 */
@Data
public class AiConversationRequest {
    private String title;
}
