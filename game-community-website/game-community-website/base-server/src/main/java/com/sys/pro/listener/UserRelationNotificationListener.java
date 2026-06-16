package com.sys.pro.listener;

import com.sys.pro.event.UserFollowedEvent;
import com.sys.pro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户关系通知监听器。
 * <p>
 * 关注关系保存成功后再创建站内通知，既保留关注提醒能力，
 * 也让用户关系服务和通知服务保持解耦，防止 WebSocket 推送链路形成启动环。
 */
@Component
@RequiredArgsConstructor
public class UserRelationNotificationListener {

    private final NotificationService notificationService;

    /**
     * 关注成功后给被关注用户发送站内通知。
     * <p>
     * 使用 AFTER_COMMIT 是为了避免关注事务回滚后仍然产生通知；
     * fallbackExecution 用于兼容当前部分调用链没有显式事务的场景。
     *
     * @param event 关注成功事件，包含关注者和被关注者ID
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserFollowed(UserFollowedEvent event) {
        if (event == null || event.getFollowerId() == null || event.getTargetUserId() == null) {
            return;
        }
        notificationService.createSystemNotification(
                event.getTargetUserId(),
                event.getFollowerId(),
                "FOLLOW",
                "新的关注",
                "关注了你",
                "USER",
                event.getFollowerId(),
                "/userinfo?userId=" + event.getFollowerId());
    }
}
