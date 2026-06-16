package com.sys.pro.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.pojo.UserBlacklist;

import java.util.List;

/**
 * UserBlacklistService 定义用户黑名单业务能力，供控制器和其他服务组合调用。
 */
public interface UserBlacklistService extends IService<UserBlacklist> {
    /**
     * 判断用户黑名单当前状态是否满足业务条件。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     * @return true 表示用户黑名单当前状态满足业务判断。
     */

    boolean isBlocked(Integer userId, Integer blockedUserId);
    /**
     * 汇总用户黑名单列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户黑名单列表数据。
     */

    List<UserBlacklist> listByUser(Integer userId);
    /**
     * 把指定用户加入黑名单，阻止对方继续发送私信。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     */

    void block(Integer userId, Integer blockedUserId);
    /**
     * 从黑名单中移除指定用户，恢复双方私信能力。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     */

    void unblock(Integer userId, Integer blockedUserId);
}
