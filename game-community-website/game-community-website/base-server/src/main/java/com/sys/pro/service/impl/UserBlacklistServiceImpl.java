package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.mapper.UserBlacklistMapper;
import com.sys.pro.pojo.UserBlacklist;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.UserBlacklistService;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserBlacklistServiceImpl 承接用户黑名单核心业务规则，协调数据访问、缓存、通知和外部服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBlacklistServiceImpl extends ServiceImpl<UserBlacklistMapper, UserBlacklist> implements UserBlacklistService {

    private final UserInfoService userInfoService;

    /**
     * 判断用户黑名单当前状态是否满足业务条件。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     * @return true 表示用户黑名单当前状态满足业务判断。
     */
    @Override
    public boolean isBlocked(Integer userId, Integer blockedUserId) {
        if (userId == null || blockedUserId == null) {
            return false;
        }
        return lambdaQuery()
                .eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getBlockedUserId, blockedUserId)
                .eq(UserBlacklist::getDeleted, false)
                .count() > 0;
    }

    /**
     * 汇总用户黑名单列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户黑名单列表数据。
     */
    @Override
    public List<UserBlacklist> listByUser(Integer userId) {
        List<UserBlacklist> list = lambdaQuery()
                .eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getDeleted, false)
                .orderByDesc(UserBlacklist::getCreateTime)
                .list();
        list.forEach(this::fillBlockedUserInfo);
        return list;
    }

    /**
     * 把指定用户加入黑名单，阻止对方继续发送私信。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     */
    @Override
    public void block(Integer userId, Integer blockedUserId) {
        if (blockedUserId == null || blockedUserId.equals(userId)) {
            /**
             * 完成用户黑名单中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 用户黑名单在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "不能拉黑自己");
        }

        UserBlacklist exists = lambdaQuery()
                .eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getBlockedUserId, blockedUserId)
                .last("LIMIT 1")
                .one();
        if (exists != null) {
            exists.setDeleted(false);
            exists.setCreateTime(LocalDateTime.now());
            updateById(exists);
            return;
        }

        UserBlacklist record = new UserBlacklist();
        record.setUserId(userId);
        record.setBlockedUserId(blockedUserId);
        record.setCreateTime(LocalDateTime.now());
        record.setDeleted(false);
        save(record);
    }

    /**
     * 从黑名单中移除指定用户，恢复双方私信能力。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param blockedUserId blockedUser 主键，用来定位关联业务数据。
     */
    @Override
    public void unblock(Integer userId, Integer blockedUserId) {
        UserBlacklist record = lambdaQuery()
                .eq(UserBlacklist::getUserId, userId)
                .eq(UserBlacklist::getBlockedUserId, blockedUserId)
                .eq(UserBlacklist::getDeleted, false)
                .last("LIMIT 1")
                .one();
        if (record != null) {
            record.setDeleted(true);
            updateById(record);
        }
    }

    /**
     * 完成用户黑名单中的 fillBlockedUserInfo 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param record record 字段，来源于当前接口入参或内部调用上下文。
     */
    private void fillBlockedUserInfo(UserBlacklist record) {
        UserInfo userInfo = userInfoService.getOne(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getUserId, record.getBlockedUserId())
                .last("LIMIT 1"));
        if (userInfo != null) {
            record.setNickname(userInfo.getNickname());
            record.setAvatar(userInfo.getAvatar());
        }
    }
}
