package com.sys.pro.service.impl;

import com.sys.pro.pojo.Message;
import com.sys.pro.service.MessageApplicationService;
import com.sys.pro.service.MessageService;
import com.sys.pro.socket.MyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 消息发送用例实现，避免 Controller 直接处理黑名单、默认字段和 WebSocket 推送细节。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageApplicationServiceImpl implements MessageApplicationService {

    private final MessageService messageService;
    private final MyWebSocketHandler myWebSocketHandler;

    /**
     * 完成站内私信中的 sendMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    @Override
    public void sendMessage(Message message, Integer currentUserId) {
        Message savedMessage = messageService.createOutgoingMessage(message, currentUserId);
        try {
            myWebSocketHandler.pushMessage(savedMessage);
        } catch (IOException e) {
            log.warn("push websocket message failed, receiverId: {}", savedMessage.getReceiverId(), e);
        }
    }
}
