package com.sys.pro.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.pojo.Notification;

import java.util.List;

/**
 * 用户通知服务。
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 创建评论 @ 提醒通知。
     */
    void createMentionNotification(Integer userId,
                                   Integer actorId,
                                   String content,
                                   String targetType,
                                   Integer targetId,
                                   String targetUrl);

    /**
     * 创建通用系统通知，供举报处罚、申诉进度等业务复用。
     */
    void createSystemNotification(Integer userId,
                                  Integer actorId,
                                  String type,
                                  String title,
                                  String content,
                                  String targetType,
                                  Integer targetId,
                                  String targetUrl);

    /**
     * 汇总站内通知列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 站内通知列表数据。
     */
    List<Notification> listRecent(Integer userId, Integer limit);

    /**
     * 统计当前用户未读通知数量，用于顶部角标提醒。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 站内通知统计值或主键结果。
     */
    Long unreadCount(Integer userId);

    /**
     * 将指定通知标记为已读，避免重复提醒用户。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    void markRead(Long id, Integer userId);
}
