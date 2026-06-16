package com.sys.pro.service;

import com.sys.pro.pojo.UserRelation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.vo.UserInfoVo;

import java.util.List;

/**
 * UserRelationService 定义关注粉丝关系业务能力，供控制器和其他服务组合调用。
 */
public interface UserRelationService extends IService<UserRelation> {



    /**
     * 读取关注关系的 FollowingList 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 关注关系列表数据。
     */

    List<UserInfoVo> getFollowingList(Integer userId);



    /**
     * 读取关注关系的 FollowersList 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 关注关系列表数据。
     */

    List<UserInfoVo> getFollowersList(Integer userId);

    /**
     * 完成关注关系中的 createRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userRelation userRelation 字段，来源于当前接口入参或内部调用上下文。
     */
    void createRelation(UserRelation userRelation);

    /**
     * 完成关注关系中的 deleteRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteRelation(Long id);

    /**
     * 完成关注关系中的 updateRelation 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userRelation userRelation 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateRelation(UserRelation userRelation);

    /**
     * 关注目标用户，并更新双方关注和粉丝计数。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     */
    void follow(Integer currentUserId, Integer targetUserId);

    /**
     * 取消关注目标用户，并同步刷新关系计数。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     */
    void unfollow(Integer currentUserId, Integer targetUserId);

    /**
     * 完成关注关系中的 canViewRelationList 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @param following following 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示关注关系当前状态满足业务判断。
     */
    boolean canViewRelationList(Integer currentUserId, Integer targetUserId, boolean following);
}

