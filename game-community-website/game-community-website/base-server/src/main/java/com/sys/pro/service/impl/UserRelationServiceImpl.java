package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sys.pro.event.UserFollowedEvent;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.pojo.UserRelation;
import com.sys.pro.mapper.UserRelationMapper;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.service.UserRelationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.utils.HashUtil;
import com.sys.pro.vo.UserInfoVo;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户关系服务实现，处理关注、取关、粉丝列表和实时状态刷新。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class UserRelationServiceImpl extends ServiceImpl<UserRelationMapper, UserRelation> implements UserRelationService {



    private final UserInfoService userInfoService;

    private final UserRelationMapper userRelationMapper;

    private final ApplicationEventPublisher eventPublisher;



    /**
     * 读取关注关系的 FollowingList 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 关注关系列表数据。
     */
    @Override
    public List<UserInfoVo> getFollowingList(Integer userId) {
        // 获取用户关注的所有用户ID
        List<Integer> followingIds = this.list(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getUserId, userId))
                .stream()
                .map(UserRelation::getFollowId)
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取这些用户的详细信息
        List<UserInfo> userInfos = userInfoService.list(new LambdaQueryWrapper<UserInfo>()
                .in(UserInfo::getUserId, followingIds));

        // 转换为VO对象
        return userInfos.stream().map(info -> {
            UserInfoVo vo = new UserInfoVo();
            vo.setUserId(info.getUserId());
            vo.setNickname(info.getNickname());
            vo.setAvatar(info.getAvatar());
            vo.setLevel(HashUtil.calculateLevel(info.getEx1() == null ? 0 : info.getEx1()));
            vo.setIsFollowed(true); // 这是关注列表，所以一定是已关注的
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 读取关注关系的 FollowersList 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 关注关系列表数据。
     */
    @Override
    public List<UserInfoVo> getFollowersList(Integer userId) {
        // 获取关注该用户的所有用户ID
        List<Integer> followerIds = this.list(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFollowId, userId))
                .stream()
                .map(UserRelation::getUserId)
                .collect(Collectors.toList());

        if (followerIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取这些用户的详细信息
        List<UserInfo> userInfos = userInfoService.list(new LambdaQueryWrapper<UserInfo>()
                .in(UserInfo::getUserId, followerIds));

        // 获取当前用户关注的用户列表（用于判断是否互相关注）
        List<Integer> currentUserFollowing = this.list(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getUserId, userId))
                .stream()
                .map(UserRelation::getFollowId)
                .collect(Collectors.toList());

        // 转换为VO对象
        return userInfos.stream().map(info -> {
            UserInfoVo vo = new UserInfoVo();
            vo.setUserId(info.getUserId());
            vo.setNickname(info.getNickname());
            vo.setAvatar(info.getAvatar());
            vo.setLevel(HashUtil.calculateLevel(info.getEx1() == null ? 0 : info.getEx1()));
            vo.setIsFollowed(currentUserFollowing.contains(info.getUserId())); // 判断是否互相关注
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 完成关注关系中的 createRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userRelation userRelation 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createRelation(UserRelation userRelation) {
        save(userRelation);
    }

    /**
     * 完成关注关系中的 deleteRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteRelation(Long id) {
        removeById(id);
    }

    /**
     * 完成关注关系中的 updateRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userRelation userRelation 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateRelation(UserRelation userRelation) {
        updateById(userRelation);
    }

    /**
     * 关注目标用户，并更新双方关注和粉丝计数。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     */
    @Override
    public void follow(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            /**
             * 完成关注关系中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 关注关系在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "不能关注自己");
        }

        // 关注关系必须唯一，避免前端重复点击产生重复数据。
        long existing = lambdaQuery()
                .eq(UserRelation::getUserId, currentUserId)
                .eq(UserRelation::getFollowId, targetUserId)
                .count();
        if (existing > 0) {
            /**
             * 完成关注关系中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 关注关系在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "您已经关注过该用户");
        }

        UserRelation userRelation = new UserRelation();
        userRelation.setUserId(currentUserId);
        userRelation.setFollowId(targetUserId);
        save(userRelation);
        // 关注通知通过事件监听处理，避免关注服务直接依赖通知服务形成启动循环依赖。
        eventPublisher.publishEvent(new UserFollowedEvent(currentUserId, targetUserId));
    }

    /**
     * 取消关注目标用户，并同步刷新关系计数。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     */
    @Override
    public void unfollow(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            /**
             * 完成关注关系中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 关注关系在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "不能取消关注自己");
        }

        long existing = lambdaQuery()
                .eq(UserRelation::getUserId, currentUserId)
                .eq(UserRelation::getFollowId, targetUserId)
                .count();
        if (existing == 0) {
            /**
             * 完成关注关系中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 关注关系在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "您还未关注该用户");
        }

        lambdaUpdate()
                .eq(UserRelation::getUserId, currentUserId)
                .eq(UserRelation::getFollowId, targetUserId)
                .remove();
    }

    /**
     * 完成关注关系中的 canViewRelationList 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @param following following 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示关注关系当前状态满足业务判断。
     */
    @Override
    public boolean canViewRelationList(Integer currentUserId, Integer targetUserId, boolean following) {
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            return true;
        }
        UserInfo userInfo = userInfoService.lambdaQuery().eq(UserInfo::getUserId, targetUserId).one();
        if (userInfo == null) {
            return true;
        }
        return following ? !Boolean.TRUE.equals(userInfo.getPrivacyFollow()) : !Boolean.TRUE.equals(userInfo.getPrivacyFans());
    }
}
