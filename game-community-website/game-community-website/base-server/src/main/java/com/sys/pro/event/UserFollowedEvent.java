package com.sys.pro.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户关注成功事件。
 * <p>
 * 关注关系写入数据库后发布该事件，通知、消息提醒等旁路业务通过监听器处理，
 * 避免用户关系服务直接依赖通知服务而产生 Spring Bean 循环依赖。
 */
@Getter
@AllArgsConstructor
public class UserFollowedEvent {

    /**
     * 发起关注的用户ID。
     */
    private final Integer followerId;

    /**
     * 被关注的用户ID。
     */
    private final Integer targetUserId;
}
