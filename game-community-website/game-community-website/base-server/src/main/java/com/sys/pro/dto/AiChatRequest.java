package com.sys.pro.dto;

import lombok.Data;

import java.util.List;

/**
 * AiChatRequest 封装AiChatRequest请求参数，避免控制器直接暴露数据库实体。
 */
@Data
public class AiChatRequest {
    private Long conversationId;
    private String content;
    private List<String> imageUrls;
}
