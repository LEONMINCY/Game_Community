package com.sys.pro.service;

import com.sys.pro.pojo.Message;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * MessageService 定义站内私信业务能力，供控制器和其他服务组合调用。
 */
public interface MessageService extends IService<Message> {

    /**
     * 完成站内私信中的 createOutgoingMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @param senderId sender 主键，用来定位关联业务数据。
     * @return 站内私信在该步骤产出的业务结果。
     */
    Message createOutgoingMessage(Message message, Integer senderId);

    /**
     * 完成站内私信中的 deleteMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteMessage(Long id);

    /**
     * 完成站内私信中的 updateMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     */
    void updateMessage(Message message);
    /**
     * 汇总站内私信列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param otherUserId otherUser 主键，用来定位关联业务数据。
     * @return 站内私信列表数据。
     */

    List<Message> listConversation(Integer userId, Integer otherUserId);

    /**
     * 完成站内私信中的 chatList 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 站内私信列表数据。
     */
    List<Map<String, Object>> chatList(Integer userId);

    /**
     * 统计当前用户收到但尚未阅读的私信总数，用于首页消息中心红点。
     * @param userId 当前登录用户主键。
     * @return 未读私信数量。
     */
    Long unreadCount(Integer userId);

    /**
     * 将指定会话中发给当前用户的未读私信标记为已读。
     * @param userId 当前登录用户主键。
     * @param otherUserId 会话另一方用户主键。
     */
    void markConversationRead(Integer userId, Integer otherUserId);

    /**
     * 完成站内私信中的 invalidateMessageCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     */
    void invalidateMessageCaches(Message message);
}
