package com.sys.pro.service;

import com.sys.pro.pojo.Message;

/**
 * 消息应用服务，负责发送消息时的黑名单校验、持久化和 WebSocket 推送。
 */
public interface MessageApplicationService {

    /**
     * 完成站内私信中的 sendMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    void sendMessage(Message message, Integer currentUserId);
}
