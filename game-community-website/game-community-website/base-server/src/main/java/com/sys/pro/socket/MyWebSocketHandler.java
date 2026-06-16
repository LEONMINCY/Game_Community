package com.sys.pro.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sys.pro.pojo.Message;
import com.sys.pro.pojo.Notification;
import com.sys.pro.service.MessageService;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私信 WebSocket 入口，只负责连接管理、消息推送和把请求交给消息服务处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;

    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /**
     * 完成WebSocket 推送中的 afterConnectionEstablished 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param session session 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        session.getAttributes().put("userId", userId);
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("websocket connected, userId: {}", userId);
    }

    /**
     * 完成WebSocket 推送中的 extractUserId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param session session 字段，来源于当前接口入参或内部调用上下文。
     * @return WebSocket 推送处理后的文本结果。
     */
    private String extractUserId(WebSocketSession session) {
        UriComponents uri = UriComponentsBuilder.fromUri(session.getUri()).build();
        return uri.getQueryParams().getFirst("userId");
    }

    /**
     * 完成WebSocket 推送中的 handleTextMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param session session 字段，来源于当前接口入参或内部调用上下文。
     * @param messageText messageText 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage messageText) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        log.info("websocket message from userId: {}, payload: {}", userId, messageText.getPayload());

        Message message = JSON.parseObject(messageText.getPayload(), Message.class);
        Integer senderId = resolveSenderId(message, userId);
        try {
            // 发送规则和落库交给服务层，WebSocket 层只负责把结果推给在线接收方。
            Message savedMessage = messageService.createOutgoingMessage(message, senderId);
            pushMessage(savedMessage);
        } catch (ServiceException e) {
            sendSelfNotice(senderId, message.getReceiverId(), e.getMessage());
        }
    }

    /**
     * 完成WebSocket 推送中的 pushMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     */
    public void pushMessage(Message message) throws IOException {
        if (message == null || message.getReceiverId() == null) {
            return;
        }
        pushPayload(message.getReceiverId(), toSocketPayload(message));
    }

    /**
     * 完成WebSocket 推送中的 pushNotification 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param notification notification 字段，来源于当前接口入参或内部调用上下文。
     */
    public void pushNotification(Notification notification) throws IOException {
        if (notification == null || notification.getUserId() == null) {
            return;
        }
        JSONObject payload = new JSONObject();
        payload.put("type", "notification");
        payload.put("data", notification);
        pushPayload(notification.getUserId(), payload);
    }

    /**
     * 完成WebSocket 推送中的 sendSelfNotice 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param senderId sender 主键，用来定位关联业务数据。
     * @param receiverId receiver 主键，用来定位关联业务数据。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     */
    private void sendSelfNotice(Integer senderId, Integer receiverId, String content) throws IOException {
        if (senderId == null) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(String.valueOf(senderId));
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        Message message = new Message();
        message.setSenderId(receiverId);
        message.setReceiverId(senderId);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        TextMessage textMessage = new TextMessage(JSONObject.toJSONString(toSocketPayload(message)));
        sendToSessions(userSessions, textMessage);
    }

    /**
     * 完成WebSocket 推送中的 pushPayload 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param payload 参与缓存键摘要计算的筛选参数串。
     */
    private void pushPayload(Integer userId, JSONObject payload) throws IOException {
        Set<WebSocketSession> userSessions = sessions.get(String.valueOf(userId));
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(JSONObject.toJSONString(payload));
        sendToSessions(userSessions, textMessage);
    }

    /**
     * 完成WebSocket 推送中的 sendToSessions 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userSessions userSessions 字段，来源于当前接口入参或内部调用上下文。
     * @param textMessage textMessage 字段，来源于当前接口入参或内部调用上下文。
     */
    private void sendToSessions(Set<WebSocketSession> userSessions, TextMessage textMessage) throws IOException {
        for (WebSocketSession session : userSessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            synchronized (session) {
                session.sendMessage(textMessage);
            }
        }
    }

    /**
     * 转换WebSocket 推送字段格式，便于后续计算或接口返回。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return WebSocket 推送在该步骤产出的业务结果。
     */
    private JSONObject toSocketPayload(Message message) {
        JSONObject msg = new JSONObject();
        msg.put("id", message.getId());
        msg.put("senderId", message.getSenderId());
        msg.put("receiverId", message.getReceiverId());
        msg.put("content", message.getContent());
        msg.put("readFlag", message.getReadFlag());
        LocalDateTime createTime = message.getCreateTime() == null ? LocalDateTime.now() : message.getCreateTime();
        msg.put("createTime", createTime.format(DateTimeFormatter.ISO_DATE_TIME));
        return msg;
    }

    /**
     * 完成WebSocket 推送中的 resolveSenderId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return WebSocket 推送统计值或主键结果。
     */
    private Integer resolveSenderId(Message message, String userId) {
        // 老接口可能在消息体中携带 senderId，新接口优先使用握手参数中的用户身份。
        if (message.getSenderId() != null) {
            return message.getSenderId();
        }
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            Integer senderId = Integer.valueOf(userId);
            message.setSenderId(senderId);
            return senderId;
        } catch (NumberFormatException e) {
            log.warn("invalid websocket userId: {}", userId);
            return null;
        }
    }

    /**
     * 完成WebSocket 推送中的 afterConnectionClosed 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param session session 字段，来源于当前接口入参或内部调用上下文。
     * @param status status 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            userId = extractUserId(session);
        }
        if (userId != null) {
            Set<WebSocketSession> userSessions = sessions.get(userId);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessions.remove(userId);
                }
            }
        }
        log.info("websocket closed, userId: {}", userId);
    }
}
