package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sys.pro.mapper.UserRelationMapper;
import com.sys.pro.pojo.Post;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.mapper.UserInfoMapper;
import com.sys.pro.pojo.UserRelation;
import com.sys.pro.repository.UserInfoRepository;
import com.sys.pro.service.PostService;
import com.sys.pro.service.UserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import static com.sys.pro.utils.HashUtil.calculateLevel;
import static com.sys.pro.utils.HashUtil.calculateLevelProgress;


/**
 * 用户资料服务实现，维护个人资料、等级、隐私设置和用户搜索数据。
 */
@Service

@RequiredArgsConstructor
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    private static final String AUDIT_APPROVED = "approved";

    private final UserInfoMapper userInfoMapper;

    private final UserRelationMapper userRelationMapper;

    private final UserInfoRepository userInfoRepository;

    private final PostService postService;

    /**
     * 读取用户资料的 UserInfo 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @return 用户资料在该步骤产出的业务结果。
     */
    @Override
    public UserInfo getUserInfo(Integer userId, Integer targetUserId) {
        UserInfo userInfo = userInfoMapper.getUserInfo(targetUserId);
        if (userInfo == null) {
            return null;
        }

        // 查询当前用户是否关注了目标用户
        if (userId != null) {
            userRelationMapper.selectList( new QueryWrapper<UserRelation>().eq("user_id", userId)).forEach(userRelation -> {
            // userRelation.getUserId().equals(targetUserId) && userRelation.getFollowId().equals(userId) ||
            if ( userRelation.getFollowId().equals(targetUserId) && userRelation.getUserId().equals(userId)) {
                userInfo.setFollow(true);
            }
            });
        }

        // 查询用户的关注量
        Integer followCount = userRelationMapper.selectCount(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getUserId, targetUserId));
        userInfo.setFollowCount(followCount);

        // 查询用户的粉丝量
        Integer fansCount = userRelationMapper.selectCount(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFollowId, targetUserId));
        userInfo.setFansCount(fansCount);

        decorateLevelAndClockStatus(userInfo);

        return userInfo;
    }

    /**
     * 读取用户资料的 UserInfo 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户资料在该步骤产出的业务结果。
     */
    @Override
    public UserInfo getUserInfo(Integer userId) {
        UserInfo userInfo = userInfoMapper.getUserInfo(userId);

        if (userInfo == null) {
            userInfo = new UserInfo();
        }
        // 查询用户的关注量
        Integer followCount = userRelationMapper.selectCount(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getUserId, userInfo.getUserId()));
        userInfo.setFollowCount(followCount);

        // 查询用户的粉丝量
        Integer fansCount = userRelationMapper.selectCount(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFollowId, userInfo.getUserId()));
        userInfo.setFansCount(fansCount);

        decorateLevelAndClockStatus(userInfo);
        return userInfo;
    }

    /**
     * 按用户名、昵称或手机号检索用户，供全局搜索和关注选择使用。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 用户资料列表数据。
     */
    @Override
    public List<UserInfo> searchUsers(String keyword, Integer limit, Integer currentUserId) {
        int size = limit == null ? 10 : Math.max(1, Math.min(limit, 30));
        String searchKeyword = keyword == null ? "" : keyword.trim();
        List<UserInfo> users = userInfoRepository.searchUsersFromAccountTables(searchKeyword, size, currentUserId);
        users.forEach(this::decorateLevelAndClockStatus);
        return users;
    }

    /**
     * 读取用户资料的 ProfileView 数据，供页面展示或后续业务判断。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     * @return 用户资料在该步骤产出的业务结果。
     */
    @Override
    public UserInfo getProfileView(Integer currentUserId, Integer targetUserId) {
        UserInfo userInfo = getUserInfo(currentUserId, targetUserId);
        if (userInfo == null) {
            return null;
        }
        userInfo.setFavoriteCount((int) countUserFavorites(targetUserId));
        fillPostFeedbackCounts(userInfo, targetUserId);
        applyPrivacy(userInfo, currentUserId, targetUserId);
        return userInfo;
    }

    /**
     * 读取用户资料的 OwnProfileView 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户资料在该步骤产出的业务结果。
     */
    @Override
    public UserInfo getOwnProfileView(Integer userId) {
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo == null) {
            return null;
        }
        userInfo.setFavoriteCount((int) countUserFavorites(userId));
        fillPostFeedbackCounts(userInfo, userId);
        return userInfo;
    }

    /**
     * 完成用户资料中的 updateProfile 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateProfile(Integer currentUserId, UserInfo userInfo) {
        userInfo.setId(null);
        userInfo.setUserId(currentUserId);
        update(userInfo, new LambdaUpdateWrapper<UserInfo>()
                .eq(UserInfo::getUserId, currentUserId));
    }

    /**
     * 完成用户每日签到，增加经验并刷新等级进度。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void clock(Integer userId) {
        Integer today = Integer.valueOf(LocalDate.now().toString().replace("-", ""));
        UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
                .and(wrapper -> wrapper.isNull("ex2").or().ne("ex2", today))
                .setSql("ex1 = COALESCE(ex1, 0) + 10")
                .set("ex2", today);

        if (userInfoMapper.update(null, updateWrapper) != 1) {
            /**
             * 完成用户资料中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 用户资料在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "您今天已经打过卡了，明天再来吧！");
        }
    }

    /**
     * 完成用户资料中的 decorateLevelAndClockStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     */
    private void decorateLevelAndClockStatus(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        Integer exp = userInfo.getEx1() == null ? 0 : userInfo.getEx1();
        Integer lastClockDate = userInfo.getEx2();
        Integer today = Integer.valueOf(LocalDate.now().toString().replace("-", ""));
        userInfo.setHasCheckedToday(today.equals(lastClockDate));
        userInfo.setEx2(calculateLevel(exp));
        userInfo.setEx1(calculateLevelProgress(exp));
    }

    /**
     * 完成用户资料中的 countUserFavorites 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 用户资料统计值或主键结果。
     */
    private long countUserFavorites(Integer userId) {
        return postService.count(visiblePostQuery()
                .apply("FIND_IN_SET({0}, favorites_user)", userId));
    }

    /**
     * 完成用户资料中的 fillPostFeedbackCounts 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     */
    private void fillPostFeedbackCounts(UserInfo userInfo, Integer targetUserId) {
        List<Post> posts = postService.list(visiblePostQuery()
                .eq(Post::getUserId, targetUserId));
        int favoriteTotal = posts.stream().mapToInt(post -> countCsvUsers(post.getFavoritesUser())).sum();
        int likeTotal = posts.stream().mapToInt(post -> countCsvUsers(post.getLikesUser())).sum();
        userInfo.setPostFavoriteCount(favoriteTotal);
        userInfo.setPostLikeCount(likeTotal);
        userInfo.setFavoriteLikeCount(favoriteTotal + likeTotal);
    }

    /**
     * 完成用户资料中的 applyPrivacy 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param targetUserId targetUser 主键，用来定位关联业务数据。
     */
    private void applyPrivacy(UserInfo userInfo, Integer currentUserId, Integer targetUserId) {
        if (userInfo == null || currentUserId != null && currentUserId.equals(targetUserId)) {
            return;
        }
        if (Boolean.TRUE.equals(userInfo.getPrivacyProfile())) {
            userInfo.setGender(null);
            userInfo.setPhone(null);
            userInfo.setEmail(null);
            userInfo.setAddress(null);
            userInfo.setAge(null);
        }
        if (Boolean.TRUE.equals(userInfo.getPrivacyFollow())) {
            userInfo.setFollowCount(null);
        }
        if (Boolean.TRUE.equals(userInfo.getPrivacyFans())) {
            userInfo.setFansCount(null);
        }
    }

    /**
     * 完成用户资料中的 visiblePostQuery 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 用户资料在该步骤产出的业务结果。
     */
    private LambdaQueryWrapper<Post> visiblePostQuery() {
        return new LambdaQueryWrapper<Post>()
                .eq(Post::getDeleted, false)
                .and(wrapper -> wrapper.eq(Post::getAuditStatus, AUDIT_APPROVED)
                        .or()
                        .isNull(Post::getAuditStatus)
                        .or()
                        .eq(Post::getAuditStatus, ""));
    }

    /**
     * 完成用户资料中的 countCsvUsers 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 用户资料统计值或主键结果。
     */
    private int countCsvUsers(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        int count = 0;
        for (String part : value.split(",")) {
            if (StringUtils.isNotBlank(part)) {
                count++;
            }
        }
        return count;
    }
}
