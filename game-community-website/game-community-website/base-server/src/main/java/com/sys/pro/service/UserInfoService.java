package com.sys.pro.service;

import com.sys.pro.pojo.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * UserInfoService 定义用户资料业务能力，供控制器和其他服务组合调用。
 */
public interface UserInfoService extends IService<UserInfo> {



    /**
     * 读取用户资料的 UserInfo 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @return 用户资料在该步骤产出的业务结果。
     */

    UserInfo getUserInfo(Integer userId, Integer targetUserId);



    /**
     * 读取用户资料的 UserInfo 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户资料在该步骤产出的业务结果。
     */

    UserInfo getUserInfo(Integer userId);
    /**
     * 按用户名、昵称或手机号检索用户，供全局搜索和关注选择使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 用户资料列表数据。
     */

    List<UserInfo> searchUsers(String keyword, Integer limit, Integer currentUserId);

    /**
     * 读取用户资料的 ProfileView 数据，供页面展示或后续业务判断。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @return 用户资料在该步骤产出的业务结果。
     */
    UserInfo getProfileView(Integer currentUserId, Integer targetUserId);

    /**
     * 读取用户资料的 OwnProfileView 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户资料在该步骤产出的业务结果。
     */
    UserInfo getOwnProfileView(Integer userId);

    /**
     * 完成用户资料中的 updateProfile 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateProfile(Integer currentUserId, UserInfo userInfo);

    /**
     * 完成用户每日签到，增加经验并刷新等级进度。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void clock(Integer userId);

}

