package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.NotificationMapper;
import com.sys.pro.mapper.UserRelationMapper;
import com.sys.pro.pojo.Notification;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.pojo.UserRelation;
import com.sys.pro.service.NotificationService;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.socket.MyWebSocketHandler;
import com.sys.pro.utils.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知业务实现，统一维护站内提醒的创建、查询和已读状态。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private static final int DEFAULT_LIMIT = 30;

    private final MyWebSocketHandler webSocketHandler;
    private final UserInfoService userInfoService;
    private final UserRelationMapper userRelationMapper;

    /**
     * 完成站内通知中的 createMentionNotification 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param actorId actor 主键，用来定位关联业务数据。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @param targetType targetType 字段，来源于当前接口入参或内部调用上下文。
     * @param targetId target 主键，用来定位关联业务数据。
     * @param targetUrl targetUrl 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createMentionNotification(Integer userId,
                                          Integer actorId,
                                          String content,
                                          String targetType,
                                          Integer targetId,
                                          String targetUrl) {
        if (userId == null || actorId == null || userId.equals(actorId)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActorId(actorId);
        notification.setType("MENTION");
        notification.setTitle("有人在评论中提到了你");
        notification.setContent(StringUtils.abbreviate(StringUtils.defaultString(content), 160));
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTargetUrl(targetUrl);
        notification.setReadFlag(false);
        notification.setCreateTime(LocalDateTime.now());
        notification.setDeleted(false);
        save(notification);
        pushNotification(notification);
    }

    /**
     * 完成站内通知中的 createSystemNotification 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param actorId actor 主键，用来定位关联业务数据。
     * @param type type 字段，来源于当前接口入参或内部调用上下文。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @param targetType targetType 字段，来源于当前接口入参或内部调用上下文。
     * @param targetId target 主键，用来定位关联业务数据。
     * @param targetUrl targetUrl 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createSystemNotification(Integer userId,
                                         Integer actorId,
                                         String type,
                                         String title,
                                         String content,
                                         String targetType,
                                         Integer targetId,
                                         String targetUrl) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setActorId(actorId);
        notification.setType(StringUtils.defaultIfBlank(type, "SYSTEM"));
        notification.setTitle(StringUtils.defaultIfBlank(title, "系统通知"));
        notification.setContent(StringUtils.abbreviate(StringUtils.defaultString(content), 300));
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTargetUrl(targetUrl);
        notification.setReadFlag(false);
        notification.setCreateTime(LocalDateTime.now());
        notification.setDeleted(false);
        save(notification);
        pushNotification(notification);
    }

    /**
     * 汇总站内通知列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 站内通知列表数据。
     */
    @Override
    public List<Notification> listRecent(Integer userId, Integer limit) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        int size = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, 100);
        List<Notification> notifications = lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getDeleted, false)
                .orderByAsc(Notification::getReadFlag)
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT " + size)
                .list();
        fillActorInfo(notifications);
        return notifications;
    }

    /**
     * 统计当前用户未读通知数量，用于顶部角标提醒。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 站内通知统计值或主键结果。
     */
    @Override
    public Long unreadCount(Integer userId) {
        if (userId == null) {
            return 0L;
        }
        return lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getReadFlag, false)
                .eq(Notification::getDeleted, false)
                .count()
                .longValue();
    }

    /**
     * 将指定通知标记为已读，避免重复提醒用户。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void markRead(Long id, Integer userId) {
        if (id == null || userId == null) {
            return;
        }
        lambdaUpdate()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getReadFlag, true)
                .update();
    }

    /**
     * 完成站内通知中的 pushNotification 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param notification notification 字段，来源于当前接口入参或内部调用上下文。
     */
    private void pushNotification(Notification notification) {
        try {
            fillActorInfo(Collections.singletonList(notification));
            webSocketHandler.pushNotification(notification);
        } catch (IOException e) {
            log.warn("push notification websocket failed, userId: {}", notification.getUserId(), e);
        }
    }

    /**
     * 补齐通知触发人的展示资料，让前端通知弹窗可以直接显示昵称、头像和等级。
     * @param notifications 已经查询出的通知列表。
     */
    private void fillActorInfo(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        Set<Integer> actorIds = notifications.stream()
                .map(Notification::getActorId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (actorIds.isEmpty()) {
            return;
        }
        List<UserInfo> userInfos = userInfoService.lambdaQuery()
                .in(UserInfo::getUserId, actorIds)
                .eq(UserInfo::getDeleted, false)
                .list();
        Map<Integer, UserInfo> userInfoMap = userInfos.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, item -> item, (left, right) -> left));
        Set<Integer> receiverIds = notifications.stream()
                .map(Notification::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Set<String> followedPairs = notifications.stream()
                .filter(notification -> notification.getUserId() != null && notification.getActorId() != null)
                .filter(notification -> notification.getActorId() > 0)
                .map(notification -> notification.getUserId() + ":" + notification.getActorId())
                .collect(Collectors.toSet());
        if (!followedPairs.isEmpty()) {
            followedPairs = userRelationMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                            .in(UserRelation::getUserId, receiverIds)
                            .in(UserRelation::getFollowId, actorIds))
                    .stream()
                    .filter(relation -> relation.getUserId() != null && relation.getFollowId() != null)
                    .map(relation -> relation.getUserId() + ":" + relation.getFollowId())
                    .filter(followedPairs::contains)
                    .collect(Collectors.toSet());
        }
        Set<String> finalFollowedPairs = followedPairs;
        notifications.forEach(notification -> {
            UserInfo actor = userInfoMap.get(notification.getActorId());
            if (actor == null) {
                return;
            }
            notification.setActorNickname(actor.getNickname());
            notification.setActorAvatar(actor.getAvatar());
            notification.setActorLevel(HashUtil.calculateLevel(actor.getEx1() == null ? 0 : actor.getEx1()));
            notification.setActorFollowed(finalFollowedPairs.contains(notification.getUserId() + ":" + notification.getActorId()));
        });
    }
}
