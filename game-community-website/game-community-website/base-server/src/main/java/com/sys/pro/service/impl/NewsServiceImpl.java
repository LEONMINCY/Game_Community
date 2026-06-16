package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.common.PageResult;
import com.sys.pro.dto.NewsPageDTO;
import com.sys.pro.mapper.NewsMapper;
import com.sys.pro.pojo.AiMessage;
import com.sys.pro.pojo.News;
import com.sys.pro.pojo.NewsComment;
import com.sys.pro.pojo.User;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.AiCommunityContextService;
import com.sys.pro.service.ContentReviewService;
import com.sys.pro.service.MiniMaxAiService;
import com.sys.pro.service.NewsCommentService;
import com.sys.pro.service.NewsService;
import com.sys.pro.service.NotificationService;
import com.sys.pro.service.ReportService;
import com.sys.pro.service.UserExperienceService;
import com.sys.pro.service.UserInfoService;
import com.sys.pro.service.UserRelationService;
import com.sys.pro.service.UserService;
import com.sys.pro.utils.HashUtil;
import com.sys.pro.vo.UserInfoVo;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 新闻业务服务，集中处理新闻互动、评论和展示字段补全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private static final int AI_ASSISTANT_USER_ID = -100;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s@，,：:]+)");

    private final NewsCommentService newsCommentService;
    private final UserInfoService userInfoService;
    private final UserService userService;
    private final ReportService reportService;
    private final UserExperienceService userExperienceService;
    private final UserRelationService userRelationService;
    private final NotificationService notificationService;
    private final MiniMaxAiService miniMaxAiService;
    private final AiCommunityContextService aiCommunityContextService;
    private final ContentReviewService contentReviewService;

    /**
     * 完成新闻中的 createNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void createNews(News news) {
        save(news);
    }

    /**
     * 完成新闻中的 updateNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateNews(News news) {
        updateById(news);
    }

    /**
     * 读取新闻的 Detail 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻在该步骤产出的业务结果。
     */
    @Override
    public News getDetail(Integer id, Integer currentUserId) {
        News news = getVisibleNews(id);
        if (news == null) {
            throw new ServiceException(404, "新闻不存在或已被删除");
        }
        lambdaUpdate()
                .eq(News::getId, id)
                .and(wrapper -> wrapper.eq(News::getDeleted, false).or().isNull(News::getDeleted))
                .setSql("brow_count = IFNULL(brow_count, 0) + 1")
                .update();
        news.setBrowCount((news.getBrowCount() == null ? 0 : news.getBrowCount()) + 1);
        return decorateNews(news, currentUserId);
    }

    /**
     * 完成新闻中的 deleteNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteNews(Long id) {
        removeById(id);
        newsCommentService.remove(new LambdaQueryWrapper<NewsComment>().eq(NewsComment::getNewsId, id));
    }

    /**
     * 完成新闻中的 pageNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<News> pageNews(NewsPageDTO dto, Integer currentUserId) {
        Page<News> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper.eq(News::getDeleted, false).or().isNull(News::getDeleted))
                .like(StringUtils.isNotBlank(dto.getTitle()), News::getTitle, dto.getTitle())
                .ge(dto.getStartTime() != null, News::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, News::getCreateTime, dto.getEndTime())
                .orderByDesc(News::getCreateTime);
        Page<News> paged = page(page, queryWrapper);
        paged.getRecords().forEach(news -> decorateNews(news, currentUserId));
        return PageResult.of(paged);
    }

    /**
     * 完成新闻中的 markLike 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param add add 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻在该步骤产出的业务结果。
     */
    @Override
    public News markLike(Integer id, Integer currentUserId, boolean add) {
        return updateNewsUserMark(id, currentUserId, true, add);
    }

    /**
     * 完成新闻中的 markFavorite 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param add add 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻在该步骤产出的业务结果。
     */
    @Override
    public News markFavorite(Integer id, Integer currentUserId, boolean add) {
        return updateNewsUserMark(id, currentUserId, false, add);
    }

    /**
     * 完成新闻中的 shareNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 新闻统计值或主键结果。
     */
    @Override
    public Integer shareNews(Integer id) {
        News news = getRequiredNews(id);
        news.setShareCount((news.getShareCount() == null ? 0 : news.getShareCount()) + 1);
        updateById(news);
        return news.getShareCount();
    }

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻列表数据。
     */
    @Override
    public List<News> listFavoriteNews(Integer userId, Integer currentUserId) {
        List<News> list = list(new LambdaQueryWrapper<News>()
                .apply("FIND_IN_SET({0}, favorites_user)", userId)
                .orderByDesc(News::getCreateTime));
        list.forEach(news -> decorateNews(news, currentUserId));
        return list;
    }

    /**
     * 新增评论并处理艾特通知、图片和 AI 助手回复等后续动作。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻统计值或主键结果。
     */
    @Override
    public Integer addComment(NewsComment comment, Integer currentUserId) {
        if (comment.getNewsId() == null) {
            /**
             * 完成新闻中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 新闻在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "新闻不存在");
        }
        if (StringUtils.isBlank(comment.getContent()) && StringUtils.isBlank(comment.getImageUrl())) {
            /**
             * 完成新闻中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 新闻在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "评论内容或图片不能为空");
        }
        checkCommentPermission(currentUserId);
        contentReviewService.ensurePublishable("新闻评论", comment.getContent());
        if (comment.getParentId() != null && comment.getReplyUserId() == null) {
            NewsComment parent = newsCommentService.getById(comment.getParentId());
            if (parent != null) {
                comment.setReplyUserId(parent.getUserId());
            }
        }
        if (comment.getParentId() != null && currentUserId.equals(comment.getReplyUserId())) {
            /**
             * 完成新闻中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 新闻在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "不能回复自己的评论");
        }
        comment.setUserId(currentUserId);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setDeleted(false);
        newsCommentService.save(comment);
        userExperienceService.award(currentUserId, "COMMENT", 5, 10, 50);
        handleReplyNotification(comment, currentUserId);
        handleMentions(comment, currentUserId);
        handleAiMention(comment, currentUserId);
        return comment.getId();
    }

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 新闻分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<NewsComment> listComments(NewsComment param) {
        int pageNo = param.getPageNo() == null ? 1 : param.getPageNo();
        int pageSize = param.getPageSize() == null ? 10 : param.getPageSize();
        Page<NewsComment> page = new Page<>(pageNo, pageSize);
        Page<NewsComment> paged = newsCommentService.page(page, new LambdaQueryWrapper<NewsComment>()
                .eq(NewsComment::getNewsId, param.getNewsId())
                .eq(NewsComment::getDeleted, false)
                .isNull(NewsComment::getParentId)
                .orderByDesc(NewsComment::getCreateTime));
        decorateNewsComments(paged.getRecords(), true);
        return PageResult.of(paged);
    }

    /**
     * 汇总新闻列表数据，供前端列表、下拉框或统计模块使用。
     * @param commentId 评论主键，用来定位被回复、举报或跳转的评论。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 新闻分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<NewsComment> listReplies(Integer commentId, Integer pageNo, Integer pageSize) {
        Page<NewsComment> page = new Page<>(pageNo, pageSize);
        Page<NewsComment> paged = newsCommentService.page(page, new LambdaQueryWrapper<NewsComment>()
                .eq(NewsComment::getParentId, commentId)
                .eq(NewsComment::getDeleted, false)
                .orderByAsc(NewsComment::getCreateTime));
        decorateNewsComments(paged.getRecords(), false);
        return PageResult.of(paged);
    }

    /**
     * 删除评论并同步刷新评论区展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param adminOrModerator adminOrModerator 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void deleteComment(Integer id, Integer currentUserId, boolean adminOrModerator) {
        NewsComment comment = newsCommentService.getById(id);
        if (comment == null) {
            return;
        }
        if (!comment.getUserId().equals(currentUserId) && !adminOrModerator) {
            /**
             * 完成新闻中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 新闻在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "只能删除自己的评论");
        }
        comment.setDeleted(true);
        comment.setUpdateTime(LocalDateTime.now());
        newsCommentService.updateById(comment);
    }

    /**
     * 完成新闻中的 updateNewsUserMark 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @param likeAction likeAction 字段，来源于当前接口入参或内部调用上下文。
     * @param add add 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻在该步骤产出的业务结果。
     */
    private News updateNewsUserMark(Integer id, Integer currentUserId, boolean likeAction, boolean add) {
        News news = getRequiredNews(id);
        String userIdText = String.valueOf(currentUserId);
        List<String> users = parseUserCsv(likeAction ? news.getLikesUser() : news.getFavoritesUser());
        if (add) {
            if (!users.contains(userIdText)) {
                users.add(0, userIdText);
            }
        } else {
            users.removeIf(userIdText::equals);
        }
        String joined = String.join(",", users);
        if (likeAction) {
            news.setLikesUser(joined);
        } else {
            news.setFavoritesUser(joined);
        }
        updateById(news);
        return decorateNews(getById(id), currentUserId);
    }

    /**
     * 读取新闻的 RequiredNews 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 新闻在该步骤产出的业务结果。
     */
    private News getRequiredNews(Integer id) {
        News news = getVisibleNews(id);
        if (news == null) {
            throw new ServiceException(404, "新闻不存在或已被删除");
        }
        return news;
    }

    /**
     * 按主键读取仍可展示的新闻，避免已删除或无效编号进入详情、点赞和转发流程。
     * @param id 新闻主键，来自前端路由或交互接口。
     * @return 存在且未删除的新闻记录，不存在时返回 null。
     */
    private News getVisibleNews(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<News>()
                .eq(News::getId, id)
                .and(wrapper -> wrapper.eq(News::getDeleted, false).or().isNull(News::getDeleted))
                .last("LIMIT 1"), false);
    }

    /**
     * 完成新闻中的 decorateNews 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param news news 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻在该步骤产出的业务结果。
     */
    private News decorateNews(News news, Integer currentUserId) {
        List<String> likes = parseUserCsv(news.getLikesUser());
        List<String> favorites = parseUserCsv(news.getFavoritesUser());
        String currentUser = currentUserId == null ? "" : String.valueOf(currentUserId);
        news.setLikes(likes.size());
        news.setFavorites(favorites.size());
        news.setShareCount(news.getShareCount() == null ? 0 : news.getShareCount());
        news.setIsLikes(likes.contains(currentUser));
        news.setIsFavorites(favorites.contains(currentUser));
        return news;
    }

    /**
     * 解析新闻相关输入，把原始字符串或请求参数转换成业务对象。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻列表数据。
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
     * 完成新闻中的 checkCommentPermission 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    private void checkCommentPermission(Integer userId) {
        User user = userService.getById(userId);
        if (user == null || Boolean.FALSE.equals(user.getEnableFlag())) {
            /**
             * 完成新闻中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 新闻在该步骤产出的业务结果。
             */
            throw new ServiceException(604, "账号已被封禁，无法发表评论");
        }
        LocalDateTime muteEndTime = reportService.getMuteEndTime(userId);
        if (muteEndTime != null) {
            /**
             * 完成新闻中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 新闻在该步骤产出的业务结果。
             */
            throw new ServiceException(605, "账号已被禁言，截止时间：" + muteEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    /**
     * 完成新闻中的 decorateNewsComments 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comments comments 字段，来源于当前接口入参或内部调用上下文。
     * @param loadChildren loadChildren 字段，来源于当前接口入参或内部调用上下文。
     */
    private void decorateNewsComments(List<NewsComment> comments, boolean loadChildren) {
        if (comments == null) {
            return;
        }
        for (NewsComment comment : comments) {
            if (AI_ASSISTANT_USER_ID == safeInt(comment.getUserId())) {
                comment.setNickname("AI助手");
                comment.setAvatar("");
                comment.setUserLevel(0);
            } else {
                UserInfo userInfo = userInfoService.getById(comment.getUserId());
                if (userInfo != null) {
                    comment.setNickname(userInfo.getNickname());
                    comment.setAvatar(userInfo.getAvatar());
                    comment.setUserLevel(HashUtil.calculateLevel(userInfo.getEx1() == null ? 0 : userInfo.getEx1()));
                }
            }
            if (comment.getReplyUserId() != null) {
                if (AI_ASSISTANT_USER_ID == safeInt(comment.getReplyUserId())) {
                    comment.setReplyUserNickname("AI助手");
                } else {
                    UserInfo replyUser = userInfoService.getById(comment.getReplyUserId());
                    if (replyUser != null) {
                        comment.setReplyUserNickname(replyUser.getNickname());
                    }
                }
            }
            int replyCount = newsCommentService.count(new LambdaQueryWrapper<NewsComment>()
                    .eq(NewsComment::getParentId, comment.getId())
                    .eq(NewsComment::getDeleted, false));
            comment.setReplyCount(replyCount);
            if (loadChildren && replyCount > 0) {
                comment.setChildren(listReplies(comment.getId(), 1, 3).getList());
            }
        }
    }

    /**
     * 完成新闻中的 handleMentions 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    private void handleMentions(NewsComment comment, Integer currentUserId) {
        Set<Integer> mentionedUserIds = resolveMentionedUsers(comment, currentUserId);
        for (Integer mentionedUserId : mentionedUserIds) {
            notificationService.createMentionNotification(
                    mentionedUserId,
                    currentUserId,
                    comment.getContent(),
                    "NEWS_COMMENT",
                    comment.getId(),
                    "/news/detail?id=" + comment.getNewsId() + "&commentId=" + comment.getId());
        }
    }

    /**
     * 新闻评论被回复时提醒原评论作者，点击通知可回到对应新闻评论。
     * @param comment 新发布的新闻回复。
     * @param currentUserId 当前回复者ID。
     */
    private void handleReplyNotification(NewsComment comment, Integer currentUserId) {
        if (comment.getParentId() == null || comment.getReplyUserId() == null || comment.getReplyUserId().equals(currentUserId)) {
            return;
        }
        notificationService.createSystemNotification(
                comment.getReplyUserId(),
                currentUserId,
                "NEWS_COMMENT_REPLY",
                "有人回复了你的新闻评论",
                comment.getContent(),
                "NEWS_COMMENT",
                comment.getId(),
                "/news/detail?id=" + comment.getNewsId() + "&commentId=" + comment.getId());
    }

    /**
     * 完成新闻中的 resolveMentionedUsers 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻在该步骤产出的业务结果。
     */
    private Set<Integer> resolveMentionedUsers(NewsComment comment, Integer currentUserId) {
        List<UserInfoVo> followingUsers = userRelationService.getFollowingList(currentUserId);
        if (followingUsers == null || followingUsers.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> followingIds = new LinkedHashSet<>();
        for (UserInfoVo user : followingUsers) {
            if (user.getUserId() != null) {
                followingIds.add(user.getUserId());
            }
        }

        Set<Integer> result = new LinkedHashSet<>();
        if (comment.getMentionUserIds() != null) {
            for (Integer userId : comment.getMentionUserIds()) {
                if (followingIds.contains(userId)) {
                    result.add(userId);
                }
            }
        }

        List<String> mentionedNames = extractMentionNames(comment.getContent());
        for (UserInfoVo user : followingUsers) {
            if (user.getUserId() != null && mentionedNames.contains(user.getNickname())) {
                result.add(user.getUserId());
            }
        }
        result.remove(currentUserId);
        return result;
    }

    /**
     * 完成新闻中的 handleAiMention 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    private void handleAiMention(NewsComment comment, Integer currentUserId) {
        if (!Boolean.TRUE.equals(comment.getMentionAi()) && !extractMentionNames(comment.getContent()).contains("AI助手")) {
            return;
        }
        NewsComment aiReply = new NewsComment();
        aiReply.setNewsId(comment.getNewsId());
        aiReply.setUserId(AI_ASSISTANT_USER_ID);
        aiReply.setContent(buildAiReply(comment, currentUserId));
        aiReply.setParentId(comment.getParentId() == null ? comment.getId() : comment.getParentId());
        aiReply.setReplyUserId(currentUserId);
        aiReply.setCreateTime(LocalDateTime.now());
        aiReply.setUpdateTime(LocalDateTime.now());
        aiReply.setDeleted(false);
        newsCommentService.save(aiReply);
    }

    /**
     * 组装新闻所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 新闻处理后的文本结果。
     */
    private String buildAiReply(NewsComment comment, Integer currentUserId) {
        String question = stripAiMention(comment.getContent());
        if (question.trim().isEmpty()) {
            question = "请结合这条新闻评论进行回复。";
        }
        AiMessage userMessage = new AiMessage();
        userMessage.setUserId(currentUserId);
        userMessage.setRole("user");
        userMessage.setContent("用户在新闻评论区@了AI助手，请直接回答评论中的问题。\n评论内容：" + question);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setDeleted(false);
        try {
            return miniMaxAiService.chat(Collections.singletonList(userMessage), aiCommunityContextService.buildContext());
        } catch (Exception e) {
            log.warn("AI news comment reply failed, commentId: {}", comment.getId(), e);
            return "我刚刚没能连接到模型，暂时无法完整回答这个问题。你可以稍后再@我一次，我会继续认真帮你分析。";
        }
    }

    /**
     * 完成新闻中的 extractMentionNames 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻列表数据。
     */
    private List<String> extractMentionNames(String content) {
        if (content == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            names.add(matcher.group(1).trim());
        }
        return names;
    }

    /**
     * 完成新闻中的 stripAiMention 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻处理后的文本结果。
     */
    private String stripAiMention(String content) {
        return content == null ? "" : content
                .replace("@AI助手", "")
                .replace("@ai助手", "")
                .replace("@AI", "")
                .trim();
    }

    /**
     * 完成新闻中的 safeInt 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 新闻统计值或主键结果。
     */
    private int safeInt(Integer value) {
        return value == null ? Integer.MIN_VALUE : value;
    }
}
