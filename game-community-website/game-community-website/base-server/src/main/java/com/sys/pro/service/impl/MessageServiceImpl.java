package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.MessageMapper;
import com.sys.pro.pojo.Message;
import com.sys.pro.pojo.User;
import com.sys.pro.pojo.UserRelation;
import com.sys.pro.service.MessageService;
import com.sys.pro.service.ContentReviewService;
import com.sys.pro.service.UserBlacklistService;
import com.sys.pro.service.UserRelationService;
import com.sys.pro.service.UserService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.RedisCacheService;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息业务服务实现，集中处理私信发送规则、聊天列表查询和消息缓存一致性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private static final String SEND_LIMIT_NOTICE =
            "当前已达到发送限制，互相关注后才能继续发送消息";
    private static final String BLACKLIST_NOTICE =
            "对方已将你加入黑名单，无法发送消息";
    private static final long MESSAGE_CACHE_SECONDS = 120;
    private static final Duration MESSAGE_CACHE_LOCK_TTL = Duration.ofSeconds(5);

    private final RedisCacheService redisCacheService;
    private final UserRelationService userRelationService;
    private final UserBlacklistService userBlacklistService;
    private final UserService userService;
    private final ContentReviewService contentReviewService;

    /**
     * 保存站内私信业务结果，并维护后续读取需要的一致状态。
     * @param entity entity 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    @Override
    public boolean save(Message entity) {
        boolean saved = super.save(entity);
        if (saved) {
            // 消息写入后清理会话缓存，避免聊天窗口和列表需要刷新才能看到新内容。
            invalidateMessageCaches(entity);
        }
        return saved;
    }

    /**
     * 按账号主键更新对应角色表中的账号信息。
     * @param entity entity 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    @Override
    public boolean updateById(Message entity) {
        Message oldMessage = entity == null || entity.getId() == null ? null : super.getById(entity.getId());
        boolean updated = super.updateById(entity);
        if (updated) {
            // 修改消息时同时清理新旧会话，避免改动了 sender/receiver 后遗留旧缓存。
            invalidateMessageCaches(oldMessage);
            invalidateMessageCaches(entity);
        }
        return updated;
    }

    /**
     * 移除站内私信中的指定数据，并同步清理关联展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    @Override
    public boolean removeById(Serializable id) {
        Message oldMessage = id == null ? null : super.getById(id);
        boolean removed = super.removeById(id);
        if (removed) {
            // 删除后让聊天列表和会话详情重新从数据库加载。
            invalidateMessageCaches(oldMessage);
        }
        return removed;
    }

    /**
     * 完成站内私信中的 createOutgoingMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @param senderId sender 主键，用来定位关联业务数据。
     * @return 站内私信在该步骤产出的业务结果。
     */
    @Override
    public Message createOutgoingMessage(Message message, Integer senderId) {
        if (message == null) {
            throw new ServiceException(500, "消息内容不能为空");
        }
        if (senderId == null) {
            throw new ServiceException(500, "发送用户不能为空");
        }
        if (message.getReceiverId() == null) {
            throw new ServiceException(500, "接收用户不能为空");
        }

        contentReviewService.ensurePublishable("私信", message.getContent());

        message.setSenderId(senderId);
        Integer receiverId = message.getReceiverId();

        // 黑名单和陌生人发送限制属于消息发送规则，统一放在服务层维护。
        if (userBlacklistService.isBlocked(receiverId, senderId)) {
            throw new ServiceException(500, BLACKLIST_NOTICE);
        }

        if (!isStaffConversation(senderId, receiverId)
                && !isMutualFollow(senderId, receiverId)
                && !isInitialMessageAllowed(senderId, receiverId)) {
            throw new ServiceException(500, SEND_LIMIT_NOTICE);
        }

        fillDefaultFields(message);
        save(message);
        return message;
    }

    /**
     * 完成站内私信中的 deleteMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteMessage(Long id) {
        removeById(id);
    }

    /**
     * 完成站内私信中的 updateMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     */
    @Override
    public void updateMessage(Message message) {
        updateById(message);
    }

    /**
     * 汇总站内私信列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param otherUserId otherUser 主键，用来定位关联业务数据。
     * @return 站内私信列表数据。
     */
    @Override
    public List<Message> listConversation(Integer userId, Integer otherUserId) {
        if (userId == null || otherUserId == null) {
            return Collections.emptyList();
        }

        String cacheKey = CacheKeys.messageConversation(userId, otherUserId);

        return redisCacheService.getOrLoadWithLock(
                cacheKey,
                "cache:" + cacheKey,
                MESSAGE_CACHE_LOCK_TTL,
                500,
                MESSAGE_CACHE_SECONDS,
                () -> lambdaQuery()
                        .and(wrapper -> wrapper
                                .eq(Message::getReceiverId, otherUserId)
                                .eq(Message::getSenderId, userId)
                                .or()
                                .eq(Message::getReceiverId, userId)
                                .eq(Message::getSenderId, otherUserId))
                        .and(wrapper -> wrapper.eq(Message::getDeleted, false).or().isNull(Message::getDeleted))
                        .orderByAsc(Message::getCreateTime)
                        .list());
    }

    /**
     * 完成站内私信中的 chatList 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 站内私信列表数据。
     */
    @Override
    public List<Map<String, Object>> chatList(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        String cacheKey = CacheKeys.messageChatList(userId);

        return redisCacheService.getOrLoadWithLock(
                cacheKey,
                "cache:" + cacheKey,
                MESSAGE_CACHE_LOCK_TTL,
                500,
                MESSAGE_CACHE_SECONDS,
                () -> buildChatList(userId));
    }

    /**
     * 统计当前用户收到但尚未阅读的私信总数，给首页消息中心红点使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 当前用户未读私信数量。
     */
    @Override
    public Long unreadCount(Integer userId) {
        if (userId == null) {
            return 0L;
        }

        Integer count = lambdaQuery()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getReadFlag, false)
                .and(wrapper -> wrapper.eq(Message::getDeleted, false).or().isNull(Message::getDeleted))
                .count();

        return count == null ? 0L : count.longValue();
    }

    /**
     * 用户打开某个会话时，将对方发给自己的未读消息统一标记为已读。
     * @param userId 当前登录用户主键。
     * @param otherUserId 会话另一方用户主键。
     */
    @Override
    public void markConversationRead(Integer userId, Integer otherUserId) {
        if (userId == null || otherUserId == null) {
            return;
        }

        boolean updated = lambdaUpdate()
                .set(Message::getReadFlag, true)
                .eq(Message::getReceiverId, userId)
                .eq(Message::getSenderId, otherUserId)
                .eq(Message::getReadFlag, false)
                .and(wrapper -> wrapper.eq(Message::getDeleted, false).or().isNull(Message::getDeleted))
                .update();

        if (updated) {
            Message cacheScope = new Message();
            cacheScope.setSenderId(otherUserId);
            cacheScope.setReceiverId(userId);
            invalidateMessageCaches(cacheScope);
        }
    }

    /**
     * 完成站内私信中的 invalidateMessageCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     */
    @Override
    public void invalidateMessageCaches(Message message) {
        if (message == null || message.getSenderId() == null || message.getReceiverId() == null) {
            return;
        }

        redisCacheService.delayedDoubleDelete(
                CacheKeys.messageConversation(message.getSenderId(), message.getReceiverId()),
                CacheKeys.messageChatList(message.getSenderId()),
                CacheKeys.messageChatList(message.getReceiverId()));
    }

    /**
     * 组装站内私信所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 站内私信列表数据。
     */
    private List<Map<String, Object>> buildChatList(Integer userId) {
        // 先按时间倒序取出当前用户所有消息，再在内存中保留每个会话的最新一条。
        List<Message> messages = lambdaQuery()
                .and(wrapper -> wrapper.eq(Message::getSenderId, userId).or().eq(Message::getReceiverId, userId))
                .and(wrapper -> wrapper.eq(Message::getDeleted, false).or().isNull(Message::getDeleted))
                .orderByDesc(Message::getCreateTime)
                .list();

        Map<Integer, Message> latestByUser = new LinkedHashMap<>();
        Map<Integer, Integer> unreadByUser = new LinkedHashMap<>();

        for (Message message : messages) {
            Integer otherUserId = userId.equals(message.getSenderId()) ? message.getReceiverId() : message.getSenderId();

            if (otherUserId != null) {
                latestByUser.putIfAbsent(otherUserId, message);

                if (userId.equals(message.getReceiverId()) && Boolean.FALSE.equals(message.getReadFlag())) {
                    unreadByUser.merge(otherUserId, 1, Integer::sum);
                }
            }
        }

        List<Map<String, Object>> chatList = new ArrayList<>();

        latestByUser.forEach((otherUserId, lastMessage) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", otherUserId);
            item.put("lastMessage", lastMessage.getContent());
            item.put("lastTime", lastMessage.getCreateTime());
            item.put("unreadCount", unreadByUser.getOrDefault(otherUserId, 0));
            chatList.add(item);
        });

        return chatList;
    }

    /**
     * 完成站内私信中的 fillDefaultFields 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     */
    private void fillDefaultFields(Message message) {
        // 入口层可能只传递文本内容，这里补齐数据库需要的默认字段。
        if (message.getCreateTime() == null) {
            message.setCreateTime(LocalDateTime.now());
        }
        if (message.getDeleted() == null) {
            message.setDeleted(false);
        }
        if (message.getReadFlag() == null) {
            message.setReadFlag(false);
        }
    }

    /**
     * 判断两名普通用户是否已经互相关注，只有双向关注才解除陌生人私信次数限制。
     * @param senderId sender 主键，用来定位关联业务数据。
     * @param receiverId receiver 主键，用来定位关联业务数据。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    private boolean isMutualFollow(Integer senderId, Integer receiverId) {
        long senderFollowing = userRelationService.lambdaQuery()
                .eq(UserRelation::getUserId, senderId)
                .eq(UserRelation::getFollowId, receiverId)
                .count();
        if (senderFollowing == 0) {
            return false;
        }
        return userRelationService.lambdaQuery()
                .eq(UserRelation::getUserId, receiverId)
                .eq(UserRelation::getFollowId, senderId)
                .count() > 0;
    }

    /**
     * 判断站内私信当前状态是否满足业务条件。
     * @param senderId sender 主键，用来定位关联业务数据。
     * @param receiverId receiver 主键，用来定位关联业务数据。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    private boolean isInitialMessageAllowed(Integer senderId, Integer receiverId) {
        return lambdaQuery()
                .eq(Message::getSenderId, senderId)
                .eq(Message::getReceiverId, receiverId)
                .and(wrapper -> wrapper.eq(Message::getDeleted, false).or().isNull(Message::getDeleted))
                .count() == 0;
    }

    /**
     * 判断站内私信当前状态是否满足业务条件。
     * @param senderId sender 主键，用来定位关联业务数据。
     * @param receiverId receiver 主键，用来定位关联业务数据。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    private boolean isStaffConversation(Integer senderId, Integer receiverId) {
        User sender = userService.getById(senderId);
        User receiver = userService.getById(receiverId);
        return isStaff(sender) || isStaff(receiver);
    }

    /**
     * 判断站内私信当前状态是否满足业务条件。
     * @param user user 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示站内私信当前状态满足业务判断。
     */
    private boolean isStaff(User user) {
        if (user == null || user.getRoleId() == null) {
            return false;
        }
        return user.getRoleId() == 1 || user.getRoleId() == 3;
    }
}
