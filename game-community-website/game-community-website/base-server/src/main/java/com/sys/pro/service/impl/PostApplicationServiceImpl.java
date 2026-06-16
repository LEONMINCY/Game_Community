package com.sys.pro.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.dto.PostPageDTO;
import com.sys.pro.pojo.BrowseHistory;
import com.sys.pro.pojo.Comment;
import com.sys.pro.pojo.Game;
import com.sys.pro.pojo.Post;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.BrowseHistoryService;
import com.sys.pro.service.CommentService;
import com.sys.pro.service.ContentReviewService;
import com.sys.pro.service.GameService;
import com.sys.pro.service.NotificationService;
import com.sys.pro.service.PostApplicationService;
import com.sys.pro.service.PostService;
import com.sys.pro.service.UserExperienceService;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.utils.CacheKeys;
import com.sys.pro.utils.HashUtil;
import com.sys.pro.utils.RedisCacheService;
import com.sys.pro.vo.PostPageVo;
import com.sys.pro.vo.PostVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子应用服务实现，承接原 Controller 中的帖子业务编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostApplicationServiceImpl implements PostApplicationService {

    private static final String AUDIT_PENDING = "pending";
    private static final String AUDIT_APPROVED = "approved";
    private static final String AUDIT_REJECTED = "rejected";
    private static final String APPEAL_NONE = "none";
    private static final String APPEAL_PENDING = "pending";
    private static final String APPEAL_APPROVED = "approved";
    private static final String APPEAL_REJECTED = "rejected";

    private final PostService postService;
    private final GameService gameService;
    private final RecommendationService recommendationService;
    private final BrowseHistoryService browseHistoryService;
    private final UserInfoService userInfoService;
    private final CommentService commentService;
    private final UserExperienceService userExperienceService;
    private final RedisCacheService redisCacheService;
    private final NotificationService notificationService;
    private final ContentReviewService contentReviewService;

    /**
     * 根据主键读取帖子详情，供详情页、弹窗或后台审核场景使用。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param adminOrModerator adminOrModerator 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<PostVo> getById(Integer id, Integer currentUserId, boolean adminOrModerator) {
        Post post = postService.getById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return CommonResult.error(500, "帖子不存在");
        }
        if (!isApproved(post) && !post.getUserId().equals(currentUserId) && !adminOrModerator) {
            return CommonResult.error(500, "帖子正在审核或未通过审核");
        }

        PostVo postVo = toPostVo(post, currentUserId);
        if (currentUserId != null) {
            saveBrowseHistory(currentUserId, id, "post");
            if (post.getGameId() != null) {
                saveBrowseHistory(currentUserId, post.getGameId(), "game");
            }
        }
        return CommonResult.success(postVo);
    }

    /**
     * 接收新增帖子数据，完成入口校验后交由业务层保存。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> create(Post post, Integer currentUserId) {
        post.setUserId(currentUserId);
        reviewPostContent(post);
        post.setTopics(normalizeTopics(post.getTopics()));
        post.setAuditStatus(AUDIT_PENDING);
        post.setAuditReason(null);
        post.setAppealStatus(APPEAL_NONE);
        post.setAppealContent(null);
        post.setAppealReply(null);
        post.setReviewTime(null);
        post.setAppealTime(null);
        post.setDeleted(false);
        postService.save(post);
        invalidatePostCaches();
        userExperienceService.award(currentUserId, "POST", 20, 3, 60);
        return CommonResult.success();
    }

    /**
     * 为后台管理端创建帖子数据，保留管理员发起操作的上下文。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> adminCreate(Post post, Integer currentUserId) {
        post.setUserId(currentUserId);
        reviewPostContent(post);
        post.setTopics(normalizeTopics(post.getTopics()));
        post.setAuditStatus(AUDIT_APPROVED);
        post.setAuditReason(null);
        post.setAppealStatus(APPEAL_NONE);
        post.setReviewTime(LocalDateTime.now());
        post.setDeleted(false);
        postService.save(post);
        invalidatePostCaches();
        return CommonResult.success();
    }

    /**
     * 按主键删除帖子记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> delete(Long id) {
        postService.removeById(id);
        commentService.remove(new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, id));
        browseHistoryService.remove(new LambdaQueryWrapper<BrowseHistory>().eq(BrowseHistory::getTargetId, id));
        invalidatePostCaches();
        return CommonResult.success();
    }

    /**
     * 保存帖子编辑后的内容，让前台展示和后台管理保持一致。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> update(Post post) {
        if (post.getTopics() != null) {
            post.setTopics(normalizeTopics(post.getTopics()));
        }
        postService.updateById(post);
        invalidatePostCaches();
        return CommonResult.success();
    }

    /**
     * 更新当前用户自己的帖子内容，并沿用个人帖子管理的权限边界。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> updateMyPost(Post post, Integer currentUserId) {
        Post dbPost = postService.getById(post.getId());
        if (dbPost == null || Boolean.TRUE.equals(dbPost.getDeleted())) {
            return CommonResult.error(500, "帖子不存在");
        }
        if (!dbPost.getUserId().equals(currentUserId)) {
            return CommonResult.error(500, "只能编辑自己的帖子");
        }
        post.setUserId(currentUserId);
        reviewPostContent(post);
        if (post.getTopics() != null) {
            post.setTopics(normalizeTopics(post.getTopics()));
        }
        post.setAuditStatus(AUDIT_PENDING);
        post.setAuditReason(null);
        post.setAppealStatus(APPEAL_NONE);
        post.setAppealContent(null);
        post.setAppealReply(null);
        post.setReviewTime(null);
        post.setAppealTime(null);
        postService.updateById(post);
        invalidatePostCaches();
        return CommonResult.success();
    }

    /**
     * 删除当前用户自己的帖子，避免用户越权操作他人内容。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> deleteMyPost(Integer id, Integer currentUserId) {
        Post post = postService.getById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return CommonResult.error(500, "帖子不存在");
        }
        if (!post.getUserId().equals(currentUserId)) {
            return CommonResult.error(500, "只能删除自己的帖子");
        }
        return delete(Long.valueOf(id));
    }

    /**
     * 记录当前用户对帖子的点赞行为，并交由业务层维护计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> like(Long id, Integer currentUserId) {
        return updatePostUsers(id, currentUserId, true, true);
    }

    /**
     * 取消当前用户对帖子的点赞关系，并刷新对应计数。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> unlike(Long id, Integer currentUserId) {
        return updatePostUsers(id, currentUserId, true, false);
    }

    /**
     * 将帖子加入当前用户收藏列表，供个人中心后续查看。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> favorite(Long id, Integer currentUserId) {
        return updatePostUsers(id, currentUserId, false, true);
    }

    /**
     * 从当前用户收藏列表移除帖子，并同步收藏状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> unFavorite(Long id, Integer currentUserId) {
        return updatePostUsers(id, currentUserId, false, false);
    }

    /**
     * 记录帖子转发次数，便于前台展示转发热度。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Integer> sharePost(Integer id) {
        Post post = postService.getById(id);
        if (!isApproved(post)) {
            return CommonResult.error(500, "帖子未通过审核，暂不能转发");
        }
        post.setShareCount((post.getShareCount() == null ? 0 : post.getShareCount()) + 1);
        postService.updateById(post);
        invalidatePostCaches();
        return CommonResult.success(post.getShareCount());
    }

    /**
     * 根据用户行为和热度指标生成帖子推荐列表。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> recommend(Integer currentUserId) {
        List<Long> gameIds = currentUserId == null ? Collections.emptyList() : recommendationService.recommendGames(currentUserId.longValue());
        List<Post> posts = buildRecommendedPosts(gameIds, 20);
        return CommonResult.success(posts.stream().map(item -> toPostVo(item, currentUserId)).collect(Collectors.toList()));
    }

    /**
     * 分页查询帖子数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<PageResult<PostPageVo>> page(PostPageDTO dto) {
        return CommonResult.success(redisCacheService.getOrLoad(CacheKeys.postPage(JSON.toJSONString(dto)), 90,
                () -> postService.pagePost(dto)));
    }

    /**
     * 按关键词检索帖子数据，给全局搜索或筛选框提供结果。
     * @param keyword 搜索关键词，支持按名称、内容或话题匹配。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> search(String keyword, Integer gameId, Integer limit, Integer currentUserId) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        int size = Math.max(1, Math.min(limit == null ? 20 : limit, 50));
        LambdaQueryWrapper<Post> wrapper = visiblePostQuery();
        if (gameId != null) {
            wrapper.eq(Post::getGameId, gameId);
        }
        if (StringUtils.isNotBlank(searchKeyword)) {
            List<Integer> gameIds = gameService.list(new QueryWrapper<Game>().like("name", searchKeyword))
                    .stream()
                    .map(Game::getId)
                    .collect(Collectors.toList());
            wrapper.and(item -> {
                item.like(Post::getTitle, searchKeyword)
                        .or()
                        .like(Post::getContent, searchKeyword)
                        .or()
                        .like(Post::getTopics, searchKeyword);
                if (!gameIds.isEmpty()) {
                    item.or().in(Post::getGameId, gameIds);
                }
            });
        }
        wrapper.orderByDesc(Post::getCreateTime).last("limit " + size);
        return CommonResult.success(postService.list(wrapper).stream()
                .map(item -> toPostVo(item, currentUserId))
                .collect(Collectors.toList()));
    }

    /**
     * 统计近期高频话题，给搜索面板和社区热议区域展示。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<Map<String, Object>>> hotTopics(Integer limit) {
        int size = limit == null ? 10 : Math.max(1, limit);
        List<Map<String, Object>> result = redisCacheService.getOrLoad(CacheKeys.postHotTopics(limit), 300, () -> {
            Map<String, Long> grouped = postService.list(visiblePostQuery())
                    .stream()
                    .flatMap(post -> splitTopics(post.getTopics()).stream())
                    .collect(Collectors.groupingBy(topic -> topic, Collectors.counting()));
            return grouped.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(size)
                    .map(entry -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("topic", entry.getKey());
                        item.put("count", entry.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());
        });
        return CommonResult.success(result);
    }

    /**
     * 审核帖子发布状态，决定内容是否可以出现在社区列表中。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> auditPost(Integer id, Map<String, String> body) {
        String status = body.get("auditStatus");
        if (!AUDIT_APPROVED.equals(status) && !AUDIT_REJECTED.equals(status)) {
            return CommonResult.error(500, "审核状态不正确");
        }
        Post post = postService.getById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return CommonResult.error(500, "帖子不存在");
        }
        post.setAuditStatus(status);
        post.setAuditReason(AUDIT_REJECTED.equals(status) ? body.get("auditReason") : null);
        post.setReviewTime(LocalDateTime.now());
        if (AUDIT_APPROVED.equals(status)) {
            post.setAppealStatus(APPEAL_NONE);
            post.setAppealReply(null);
        }
        postService.updateById(post);
        invalidatePostCaches();
        notifyPostAuditResult(post, status);
        return CommonResult.success();
    }

    /**
     * 提交帖子审核拒绝后的申诉内容。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> appealPost(Integer id, Map<String, String> body, Integer currentUserId) {
        Post post = postService.getById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return CommonResult.error(500, "帖子不存在");
        }
        if (!post.getUserId().equals(currentUserId)) {
            return CommonResult.error(500, "只能申诉自己的帖子");
        }
        if (!AUDIT_REJECTED.equals(post.getAuditStatus())) {
            return CommonResult.error(500, "只有审核拒绝的帖子可以申诉");
        }
        String content = body.get("appealContent");
        if (StringUtils.isBlank(content)) {
            return CommonResult.error(500, "请填写申诉内容");
        }
        post.setAppealContent(content);
        post.setAppealStatus(APPEAL_PENDING);
        post.setAppealReply(null);
        post.setAppealTime(LocalDateTime.now());
        postService.updateById(post);
        invalidatePostCaches();
        return CommonResult.success();
    }

    /**
     * 处理帖子申诉结果，并回写审核进度。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param body 请求体数据，承载本次操作需要的业务字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<Void> auditAppeal(Integer id, Map<String, String> body) {
        String status = body.get("appealStatus");
        if (!APPEAL_APPROVED.equals(status) && !APPEAL_REJECTED.equals(status)) {
            return CommonResult.error(500, "申诉处理状态不正确");
        }
        Post post = postService.getById(id);
        if (post == null || Boolean.TRUE.equals(post.getDeleted())) {
            return CommonResult.error(500, "帖子不存在");
        }
        if (!APPEAL_PENDING.equals(post.getAppealStatus())) {
            return CommonResult.error(500, "当前帖子没有待处理申诉");
        }
        post.setAppealStatus(status);
        post.setAppealReply(body.get("appealReply"));
        post.setReviewTime(LocalDateTime.now());
        if (APPEAL_APPROVED.equals(status)) {
            post.setAuditStatus(AUDIT_APPROVED);
            post.setAuditReason(null);
        }
        postService.updateById(post);
        invalidatePostCaches();
        notifyPostAppealResult(post, status);
        return CommonResult.success();
    }

    /**
     * 按游戏标签查询社区帖子，展示对应游戏下的讨论内容。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> gamePost(Integer gameId, Integer currentUserId) {
        List<PostVo> posts = redisCacheService.getOrLoad(CacheKeys.postGame(gameId) + ":user:" + (currentUserId == null ? "guest" : currentUserId), 60, () ->
                postService.list(visiblePostQuery().eq(Post::getGameId, gameId).orderByDesc(Post::getCreateTime).last("limit 20"))
                        .stream()
                        .map(item -> toPostVo(item, currentUserId))
                        .collect(Collectors.toList()));
        return CommonResult.success(posts);
    }

    /**
     * 查询指定用户公开的帖子动态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> userPost(Integer userId, Integer currentUserId) {
        return CommonResult.success(postService.list(visiblePostQuery()
                        .eq(Post::getUserId, userId)
                        .orderByDesc(Post::getCreateTime)
                        .last("limit 20"))
                .stream()
                .map(item -> toPostVo(item, currentUserId))
                .collect(Collectors.toList()));
    }

    /**
     * 查询当前用户发布的帖子，供我的帖子页面管理。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param auditStatus auditStatus 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> myPosts(String title, String auditStatus, Integer currentUserId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, currentUserId)
                .eq(Post::getDeleted, false);
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(Post::getTitle, title);
        }
        if (StringUtils.isNotBlank(auditStatus)) {
            wrapper.eq(Post::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Post::getCreateTime);
        return CommonResult.success(postService.list(wrapper).stream()
                .map(item -> toPostVo(item, currentUserId))
                .collect(Collectors.toList()));
    }

    /**
     * 按游戏主键读取关联帖子，支撑游戏详情页社区讨论区。
     * @param gameId 游戏主键，用来定位被操作的游戏。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> getPostsByGameId(Long gameId, Integer currentUserId) {
        List<PostVo> posts = redisCacheService.getOrLoad(CacheKeys.postGameAll(gameId) + ":user:" + (currentUserId == null ? "guest" : currentUserId), 60, () ->
                postService.list(visiblePostQuery().eq(Post::getGameId, gameId).orderByDesc(Post::getCreateTime))
                        .stream()
                        .limit(20)
                        .map(item -> toPostVo(item, currentUserId))
                        .collect(Collectors.toList()));
        return CommonResult.success(posts);
    }

    /**
     * 读取帖子的 PostsByGameName 数据，供页面展示或后续业务判断。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> getPostsByGameName(String name, Integer currentUserId) {
        if (StringUtils.isBlank(name)) {
            return CommonResult.success(Collections.emptyList());
        }
        List<Game> games = gameService.list(new QueryWrapper<Game>().like("name", name));
        if (games.isEmpty()) {
            return CommonResult.success(Collections.emptyList());
        }
        List<Integer> gameIds = games.stream().map(Game::getId).collect(Collectors.toList());
        return CommonResult.success(postService.list(visiblePostQuery().in(Post::getGameId, gameIds).orderByDesc(Post::getCreateTime))
                .stream()
                .limit(20)
                .map(item -> toPostVo(item, currentUserId))
                .collect(Collectors.toList()));
    }

    /**
     * 读取帖子的 Favorites 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @Override
    public CommonResult<List<PostVo>> getFavorites(Integer userId) {
        return CommonResult.success(postService.getFavoritesByUserId(userId));
    }

    /**
     * 完成帖子中的 updatePostUsers 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param likeAction likeAction 字段，来源于当前接口入参或内部调用上下文。
     * @param add add 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    private CommonResult<Void> updatePostUsers(Long id, Integer currentUserId, boolean likeAction, boolean add) {
        Post post = postService.getById(id);
        if (!isApproved(post)) {
            return CommonResult.error(500, "帖子未通过审核，暂不能操作");
        }
        String userIdText = currentUserId.toString();
        List<String> users = parseUserCsv(likeAction ? post.getLikesUser() : post.getFavoritesUser());
        boolean contains = users.contains(userIdText);
        if (add && contains) {
            return CommonResult.error(500, likeAction ? "您已经点赞过了" : "您已经收藏过了");
        }
        if (!add && !contains) {
            return CommonResult.error(500, likeAction ? "您还未点赞" : "您还未收藏");
        }
        if (add) {
            users = new ArrayList<>(users);
            users.add(0, userIdText);
        } else {
            users = users.stream().filter(user -> !user.equals(userIdText)).collect(Collectors.toList());
        }
        if (likeAction) {
            post.setLikesUser(String.join(",", users));
        } else {
            post.setFavoritesUser(String.join(",", users));
        }
        postService.updateById(post);
        invalidatePostCaches();
        if (add && likeAction) {
            notifyPostLiked(post, currentUserId);
        }
        return CommonResult.success();
    }

    /**
     * 转换帖子字段格式，便于后续计算或接口返回。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 帖子在该步骤产出的业务结果。
     */
    private PostVo toPostVo(Post post, Integer currentUserId) {
        PostVo postVo = new PostVo();
        BeanUtil.copyProperties(post, postVo);
        normalizeAuditInfo(postVo);

        List<String> likeUsers = parseUserCsv(post.getLikesUser());
        List<String> favoriteUsers = parseUserCsv(post.getFavoritesUser());
        String currentUser = currentUserId == null ? "" : currentUserId.toString();
        postVo.setLikes(likeUsers.size());
        postVo.setFavorites(favoriteUsers.size());
        postVo.setShareCount(post.getShareCount() == null ? 0 : post.getShareCount());
        postVo.setCommentCount(Math.toIntExact(commentService.count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, post.getId())
                .eq(Comment::getDeleted, false))));
        postVo.setIsLikes(StringUtils.isNotBlank(currentUser) && likeUsers.contains(currentUser));
        postVo.setIsFavorites(StringUtils.isNotBlank(currentUser) && favoriteUsers.contains(currentUser));

        Game game = gameService.getById(postVo.getGameId());
        if (game != null) {
            postVo.setGameName(game.getName());
            postVo.setGameIcon(game.getIcon());
        }
        UserInfo userInfo = userInfoService.getById(post.getUserId());
        if (userInfo != null) {
            postVo.setNickname(userInfo.getNickname());
            postVo.setUserAvatar(userInfo.getAvatar());
            postVo.setUserLevel(HashUtil.calculateLevel(userInfo.getEx1() == null ? 0 : userInfo.getEx1()));
        }
        return postVo;
    }

    /**
     * 完成帖子中的 normalizeAuditInfo 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param postVo postVo 字段，来源于当前接口入参或内部调用上下文。
     */
    private void normalizeAuditInfo(PostVo postVo) {
        if (StringUtils.isBlank(postVo.getAuditStatus())) {
            postVo.setAuditStatus(AUDIT_APPROVED);
        }
        if (StringUtils.isBlank(postVo.getAppealStatus())) {
            postVo.setAppealStatus(APPEAL_NONE);
        }
    }

    /**
     * 完成帖子中的 visiblePostQuery 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 帖子在该步骤产出的业务结果。
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
     * 判断帖子当前状态是否满足业务条件。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示帖子当前状态满足业务判断。
     */
    private boolean isApproved(Post post) {
        return post != null && (StringUtils.isBlank(post.getAuditStatus()) || AUDIT_APPROVED.equals(post.getAuditStatus()));
    }

    /**
     * 完成帖子中的 normalizeTopics 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param topics topics 字段，来源于当前接口入参或内部调用上下文。
     * @return 帖子处理后的文本结果。
     */
    private String normalizeTopics(String topics) {
        if (StringUtils.isBlank(topics)) {
            return "";
        }
        return splitTopics(topics).stream().distinct().collect(Collectors.joining(","));
    }

    /**
     * 完成帖子中的 splitTopics 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param topics topics 字段，来源于当前接口入参或内部调用上下文。
     * @return 帖子列表数据。
     */
    private List<String> splitTopics(String topics) {
        if (StringUtils.isBlank(topics)) {
            return new ArrayList<>();
        }
        return Arrays.stream(topics.split("[,#，、\\s]+"))
                .map(topic -> topic.replace("#", "").trim())
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 组装帖子所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param gameIds 游戏主键集合，表示本次批量处理的游戏范围。
     * @param limit 返回数量上限，避免一次加载过多数据。
     * @return 帖子列表数据。
     */
    private List<Post> buildRecommendedPosts(List<Long> gameIds, int limit) {
        int size = Math.max(1, limit);
        List<Post> allPosts = postService.list(visiblePostQuery());
        if (gameIds == null || gameIds.isEmpty()) {
            return rankPosts(allPosts).stream().limit(size).collect(Collectors.toList());
        }
        Map<Integer, Integer> priority = new HashMap<>();
        for (int i = 0; i < gameIds.size(); i++) {
            priority.put(gameIds.get(i).intValue(), i);
        }
        Set<Integer> selectedGameIds = new HashSet<>(priority.keySet());
        List<Post> recommended = allPosts.stream()
                .filter(post -> post.getGameId() != null && selectedGameIds.contains(post.getGameId()))
                .sorted(Comparator
                        .comparingInt((Post post) -> priority.getOrDefault(post.getGameId(), Integer.MAX_VALUE))
                        .thenComparing(this::postRecommendScore, Comparator.reverseOrder()))
                .limit(size)
                .collect(Collectors.toList());
        if (recommended.size() < size) {
            Set<Integer> selectedPostIds = recommended.stream().map(Post::getId).collect(Collectors.toSet());
            recommended.addAll(rankPosts(allPosts).stream()
                    .filter(post -> !selectedPostIds.contains(post.getId()))
                    .limit(size - recommended.size())
                    .collect(Collectors.toList()));
        }
        return recommended;
    }

    /**
     * 完成帖子中的 rankPosts 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param posts posts 字段，来源于当前接口入参或内部调用上下文。
     * @return 帖子列表数据。
     */
    private List<Post> rankPosts(List<Post> posts) {
        return posts.stream()
                .sorted(Comparator
                        .comparingDouble(this::postRecommendScore)
                        .reversed()
                        .thenComparing(Post::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 完成帖子中的 postRecommendScore 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @return 帖子在该步骤产出的业务结果。
     */
    private double postRecommendScore(Post post) {
        int likeCount = parseUserCsv(post.getLikesUser()).size();
        int favoriteCount = parseUserCsv(post.getFavoritesUser()).size();
        int shareCount = post.getShareCount() == null ? 0 : post.getShareCount();
        double recency = post.getCreateTime() == null ? 0 : post.getCreateTime().toLocalDate().toEpochDay() / 10000.0;
        return likeCount * 3.0 + favoriteCount * 4.0 + shareCount * 2.0 + recency;
    }

    /**
     * 解析帖子相关输入，把原始字符串或请求参数转换成业务对象。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 帖子列表数据。
     */
    private List<String> parseUserCsv(String value) {
        if (StringUtils.isBlank(value)) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 保存帖子业务结果，并维护后续读取需要的一致状态。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param targetId target 主键，用来定位关联业务数据。
     * @param targetType targetType 字段，来源于当前接口入参或内部调用上下文。
     */
    private void saveBrowseHistory(Integer userId, Integer targetId, String targetType) {
        BrowseHistory history = new BrowseHistory();
        history.setUserId(userId);
        history.setTargetId(targetId);
        history.setTargetType(targetType);
        browseHistoryService.save(history);
    }

    /**
     * 完成帖子中的 invalidatePostCaches 步骤，保证该环节的数据和状态可以继续向下流转。
     */
    private void invalidatePostCaches() {
        redisCacheService.delayedDoubleDeleteByPattern(CacheKeys.POST_PATTERN, CacheKeys.GAME_PATTERN);
    }

    /**
     * 给帖子作者发送点赞通知，通知中心可点击跳回被点赞的帖子。
     * @param post 被点赞的帖子。
     * @param actorId 点赞用户ID。
     */
    private void notifyPostLiked(Post post, Integer actorId) {
        if (post == null || post.getUserId() == null || actorId == null || post.getUserId().equals(actorId)) {
            return;
        }
        notificationService.createSystemNotification(
                post.getUserId(),
                actorId,
                "POST_LIKE",
                "有人点赞了你的帖子",
                "点赞了你的帖子《" + StringUtils.defaultString(post.getTitle(), "未命名帖子") + "》",
                "POST",
                post.getId(),
                "/forum?id=" + post.getId());
    }

    /**
     * 完成帖子中的 notifyPostAuditResult 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param status status 字段，来源于当前接口入参或内部调用上下文。
     */
    private void notifyPostAuditResult(Post post, String status) {
        String statusText = AUDIT_APPROVED.equals(status) ? "已通过" : "未通过";
        notificationService.createSystemNotification(
                post.getUserId(),
                null,
                "POST_AUDIT",
                "帖子审核" + statusText,
                "你的帖子《" + StringUtils.defaultString(post.getTitle(), "未命名帖子") + "》审核" + statusText
                        + "。原因：" + StringUtils.defaultIfBlank(post.getAuditReason(), "无"),
                "POST",
                post.getId(),
                "/myPosts");
    }

    /**
     * 完成帖子中的 notifyPostAppealResult 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     * @param status status 字段，来源于当前接口入参或内部调用上下文。
     */
    private void notifyPostAppealResult(Post post, String status) {
        String statusText = APPEAL_APPROVED.equals(status) ? "已通过" : "已驳回";
        notificationService.createSystemNotification(
                post.getUserId(),
                null,
                "POST_APPEAL",
                "帖子申诉" + statusText,
                "你的帖子《" + StringUtils.defaultString(post.getTitle(), "未命名帖子") + "》申诉" + statusText
                        + "。审核回复：" + StringUtils.defaultIfBlank(post.getAppealReply(), "无"),
                "POST",
                post.getId(),
                "/myPosts");
    }

    /**
     * 完成帖子中的 reviewPostContent 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param post post 字段，来源于当前接口入参或内部调用上下文。
     */
    private void reviewPostContent(Post post) {
        contentReviewService.ensurePublishable(
                "帖子",
                StringUtils.defaultString(post.getTitle()) + "\n" + StringUtils.defaultString(post.getContent()));
    }
}
