package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.AiMessage;
import com.sys.pro.pojo.Comment;
import com.sys.pro.pojo.Post;
import com.sys.pro.pojo.User;
import com.sys.pro.service.AiCommunityContextService;
import com.sys.pro.service.CommentService;
import com.sys.pro.service.ContentReviewService;
import com.sys.pro.service.MiniMaxAiService;
import com.sys.pro.service.NotificationService;
import com.sys.pro.service.ReportService;
import com.sys.pro.service.UserExperienceService;
import com.sys.pro.service.UserRelationService;
import com.sys.pro.service.UserService;
import com.sys.pro.mapper.CommentMapper;
import com.sys.pro.mapper.PostMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.vo.CommentVo;
import com.sys.pro.vo.UserInfoVo;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sys.pro.utils.HashUtil.calculateLevel;

/**
 * 评论服务实现，处理帖子评论、回复、图片表情、艾特通知和 AI 回复。
 */
@Slf4j

@Service

@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private static final int AI_ASSISTANT_USER_ID = -100;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^\\s@，,：:]+)");

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserService userService;
    private final ReportService reportService;
    private final UserExperienceService userExperienceService;
    private final UserRelationService userRelationService;
    private final NotificationService notificationService;
    private final MiniMaxAiService miniMaxAiService;
    private final AiCommunityContextService aiCommunityContextService;
    private final ContentReviewService contentReviewService;

    /**
     * 读取评论的 PageListByPostId 数据，供页面展示或后续业务判断。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 评论分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<CommentVo> getPageListByPostId(CommentVo param) {
        Page<CommentVo> page = new Page<>(param.getPageNo(), param.getPageSize());
        // 只获取一级评论（parentId为null的评论）
        param.setParentId(null);
        page = commentMapper.getPageListByPostId(page, param);
        
        // 获取每个评论的子评论
        List<CommentVo> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            for (CommentVo comment : records) {
                if (comment.getReplyCount() != null && comment.getReplyCount() > 0) {
                    // 获取前3条回复
                    PageResult<CommentVo> replyList = getReplyList(comment.getId(), 1, 3, param.getCurrentUserId());
                    comment.setChildren(replyList.getList());
                }
            }
            decorateCommentLevels(records);
        }
        
        return PageResult.of(page);
    }

    /**
     * 新增评论并处理艾特通知、图片和 AI 助手回复等后续动作。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @return 评论统计值或主键结果。
     */
    @Override
    public Integer addComment(Comment comment) {
        User user = userService.getById(comment.getUserId());
        if (user == null || Boolean.FALSE.equals(user.getEnableFlag())) {
            /**
             * 完成评论中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 评论在该步骤产出的业务结果。
             */
            throw new ServiceException(604, "账号已被封禁，无法发表评论");
        }
        LocalDateTime muteEndTime = reportService.getMuteEndTime(comment.getUserId());
        if (muteEndTime != null) {
            /**
             * 完成评论中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 评论在该步骤产出的业务结果。
             */
            throw new ServiceException(605, "账号已被禁言，截止时间：" + muteEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (comment.getUserId() == null || comment.getUserId() > 0) {
            contentReviewService.ensurePublishable("评论", comment.getContent());
        }
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setDeleted(false);
        save(comment);
        return comment.getId();
    }

    /**
     * 完成评论中的 createComment 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 评论统计值或主键结果。
     */
    @Override
    public Integer createComment(Comment comment, Integer currentUserId) {
        comment.setUserId(currentUserId);
        validateReplyTarget(comment, currentUserId);
        Integer commentId = addComment(comment);
        userExperienceService.award(currentUserId, "COMMENT", 5, 10, 50);
        handlePostCommentNotification(comment, currentUserId);
        handleReplyNotification(comment, currentUserId);
        handleMentions(comment, currentUserId);
        handleAiMention(comment, currentUserId);
        return commentId;
    }

    /**
     * 删除评论并同步刷新评论区展示状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteComment(Long id) {
        removeById(id);
    }

    /**
     * 完成评论中的 updateComment 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateComment(Comment comment) {
        updateById(comment);
    }

    /**
     * 读取评论的 ReplyList 数据，供页面展示或后续业务判断。
     * @param commentId 评论主键，用来定位被回复、举报或跳转的评论。
     * @param pageNo 当前页码，用于区分不同分页缓存。
     * @param pageSize 每页数量，用于控制分页列表长度。
     * @return 评论分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<CommentVo> getReplyList(Integer commentId, Integer pageNo, Integer pageSize) {
        return getReplyList(commentId, pageNo, pageSize, null);
    }

    /**
     * 查询指定主评论下的回复，并按当前用户标记每条回复的点赞状态。
     * @param commentId 主评论ID。
     * @param pageNo 当前页码。
     * @param pageSize 每页数量。
     * @param currentUserId 当前登录用户ID，游客访问时为空。
     * @return 回复评论分页结果。
     */
    @Override
    public PageResult<CommentVo> getReplyList(Integer commentId, Integer pageNo, Integer pageSize, Integer currentUserId) {
        Page<CommentVo> page = new Page<>(pageNo, pageSize);
        CommentVo param = new CommentVo();
        param.setParentId(commentId);
        param.setCurrentUserId(currentUserId);
        page = commentMapper.getPageListByParentId(page, param);
        decorateCommentLevels(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 查询当前用户发表过的评论，并补齐评论作者等级与所属帖子摘要。
     * @param userId 当前登录用户主键。
     * @param pageNo 当前页码，用于分页加载。
     * @param pageSize 每页数量，用于控制单次返回规模。
     * @return 评论分页结果，包含评论内容和所属帖子信息。
     */
    @Override
    public PageResult<CommentVo> getMyCommentList(Integer userId, Integer pageNo, Integer pageSize) {
        if (userId == null) {
            return PageResult.of(new Page<>(pageNo, pageSize));
        }
        Page<CommentVo> page = commentMapper.getMyCommentList(new Page<>(pageNo, pageSize), userId);
        decorateCommentLevels(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 点赞评论并通知评论作者，使用用户ID集合保证同一用户只能点赞一次。
     * @param commentId 被点赞的评论ID。
     * @param currentUserId 当前登录用户ID。
     * @return 点赞后的点赞总数。
     */
    @Override
    public Integer likeComment(Integer commentId, Integer currentUserId) {
        Comment comment = getOperableComment(commentId, currentUserId);
        if (currentUserId.equals(comment.getUserId())) {
            throw new ServiceException(500, "不能点赞自己的评论");
        }
        Set<Integer> likedUserIds = parseLikeUserIds(comment.getLikesUser());
        boolean added = likedUserIds.add(currentUserId);
        Comment update = new Comment();
        update.setId(commentId);
        update.setLikesUser(joinLikeUserIds(likedUserIds));
        updateById(update);
        if (added && comment.getUserId() != null) {
            notificationService.createSystemNotification(
                    comment.getUserId(),
                    currentUserId,
                    "COMMENT_LIKE",
                    "有人点赞了你的评论",
                    abbreviate(comment.getContent(), 120),
                    "POST_COMMENT",
                    comment.getId(),
                    "/forum?id=" + comment.getPostId() + "&commentId=" + comment.getId());
        }
        return likedUserIds.size();
    }

    /**
     * 取消评论点赞，前端重复点击取消时保持幂等，避免产生无意义异常。
     * @param commentId 被取消点赞的评论ID。
     * @param currentUserId 当前登录用户ID。
     * @return 取消后的点赞总数。
     */
    @Override
    public Integer unlikeComment(Integer commentId, Integer currentUserId) {
        Comment comment = getOperableComment(commentId, currentUserId);
        Set<Integer> likedUserIds = parseLikeUserIds(comment.getLikesUser());
        if (likedUserIds.remove(currentUserId)) {
            Comment update = new Comment();
            update.setId(commentId);
            update.setLikesUser(joinLikeUserIds(likedUserIds));
            updateById(update);
        }
        return likedUserIds.size();
    }

    /**
     * 读取可点赞的评论，并统一校验登录态、评论存在性和删除状态。
     * @param commentId 评论ID。
     * @param currentUserId 当前登录用户ID。
     * @return 可以继续处理的评论实体。
     */
    private Comment getOperableComment(Integer commentId, Integer currentUserId) {
        if (currentUserId == null) {
            throw new ServiceException(401, "请先登录后再操作");
        }
        Comment comment = getById(commentId);
        if (comment == null || Boolean.TRUE.equals(comment.getDeleted())) {
            throw new ServiceException(404, "评论不存在或已被删除");
        }
        return comment;
    }

    /**
     * 将数据库中的点赞用户字符串解析为有序集合，兼容历史空值和默认0。
     * @param likesUser 点赞用户ID字符串。
     * @return 去重后的点赞用户ID集合。
     */
    private Set<Integer> parseLikeUserIds(String likesUser) {
        Set<Integer> userIds = new LinkedHashSet<>();
        if (likesUser == null) {
            return userIds;
        }
        String normalized = likesUser.trim();
        if (normalized.isEmpty() || "0".equals(normalized)) {
            return userIds;
        }
        for (String item : normalized.split(",")) {
            try {
                Integer userId = Integer.valueOf(item.trim());
                if (userId > 0) {
                    userIds.add(userId);
                }
            } catch (NumberFormatException ignored) {
                log.debug("Ignore invalid comment like user id: {}", item);
            }
        }
        return userIds;
    }

    /**
     * 将点赞用户集合重新写回数据库字段，集合为空时写入空串。
     * @param userIds 点赞用户ID集合。
     * @return 数据库可保存的逗号分隔字符串。
     */
    private String joinLikeUserIds(Set<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Integer userId : userIds) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(userId);
        }
        return builder.toString();
    }

    /**
     * 通知内容只展示评论摘要，避免长评论撑开通知卡片。
     * @param content 原始评论内容。
     * @param maxLength 最大展示长度。
     * @return 裁剪后的评论摘要。
     */
    private String abbreviate(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    /**
     * 校验评论操作前置条件，提前阻断不合法请求。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    private void validateReplyTarget(Comment comment, Integer currentUserId) {
        if (comment.getParentId() == null) {
            return;
        }
        Integer replyUserId = comment.getReplyUserId();
        if (replyUserId == null) {
            Comment parent = getById(comment.getParentId());
            if (parent != null) {
                replyUserId = parent.getUserId();
                comment.setReplyUserId(replyUserId);
            }
        }
        if (currentUserId != null && currentUserId.equals(replyUserId)) {
            /**
             * 完成评论中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 评论在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "不能回复自己的评论");
        }
    }

    /**
     * 完成评论中的 decorateCommentLevels 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comments comments 字段，来源于当前接口入参或内部调用上下文。
     */
    private void decorateCommentLevels(List<CommentVo> comments) {
        if (comments == null) {
            return;
        }
        for (CommentVo comment : comments) {
            if (AI_ASSISTANT_USER_ID == safeInt(comment.getUserId())) {
                comment.setNickname("AI助手");
                comment.setUserLevel(0);
            } else {
                int exp = comment.getUserLevel() == null ? 0 : comment.getUserLevel();
                comment.setUserLevel(calculateLevel(exp));
            }
            if (AI_ASSISTANT_USER_ID == safeInt(comment.getReplyUserId())) {
                comment.setReplyUserNickname("AI助手");
            }
        }
    }

    /**
     * 完成评论中的 handleMentions 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    private void handleMentions(Comment comment, Integer currentUserId) {
        Set<Integer> mentionedUserIds = resolveMentionedUsers(comment, currentUserId);
        for (Integer mentionedUserId : mentionedUserIds) {
            notificationService.createMentionNotification(
                    mentionedUserId,
                    currentUserId,
                    comment.getContent(),
                    "POST_COMMENT",
                    comment.getId(),
                    "/forum?id=" + comment.getPostId() + "&commentId=" + comment.getId());
        }
    }

    /**
     * 评论被回复时提醒原评论作者，点击通知可直接定位到对应回复。
     * @param comment 新发布的回复评论。
     * @param currentUserId 当前回复者ID。
     */
    private void handleReplyNotification(Comment comment, Integer currentUserId) {
        if (comment.getParentId() == null || comment.getReplyUserId() == null || comment.getReplyUserId().equals(currentUserId)) {
            return;
        }
        notificationService.createSystemNotification(
                comment.getReplyUserId(),
                currentUserId,
                "COMMENT_REPLY",
                "有人回复了你的评论",
                comment.getContent(),
                "POST_COMMENT",
                comment.getId(),
                "/forum?id=" + comment.getPostId() + "&commentId=" + comment.getId());
    }

    /**
     * 别人直接评论帖子时提醒帖子作者，通知中心“评论我的”会展示这类消息。
     * @param comment 新发布的评论。
     * @param currentUserId 当前评论者ID。
     */
    private void handlePostCommentNotification(Comment comment, Integer currentUserId) {
        if (comment.getParentId() != null || comment.getPostId() == null) {
            return;
        }
        Post post = postMapper.selectById(comment.getPostId());
        if (post == null || post.getUserId() == null || post.getUserId().equals(currentUserId)) {
            return;
        }
        notificationService.createSystemNotification(
                post.getUserId(),
                currentUserId,
                "POST_COMMENT",
                "有人评论了你的帖子",
                comment.getContent(),
                "POST_COMMENT",
                comment.getId(),
                "/forum?id=" + comment.getPostId() + "&commentId=" + comment.getId());
    }

    /**
     * 完成评论中的 resolveMentionedUsers 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 评论在该步骤产出的业务结果。
     */
    private Set<Integer> resolveMentionedUsers(Comment comment, Integer currentUserId) {
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
     * 完成评论中的 handleAiMention 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     */
    private void handleAiMention(Comment comment, Integer currentUserId) {
        if (!shouldAiReply(comment)) {
            return;
        }
        String answer = buildAiReply(comment, currentUserId);
        Comment aiReply = new Comment();
        aiReply.setPostId(comment.getPostId());
        aiReply.setUserId(AI_ASSISTANT_USER_ID);
        aiReply.setContent(answer);
        aiReply.setParentId(comment.getParentId() == null ? comment.getId() : comment.getParentId());
        aiReply.setReplyUserId(currentUserId);
        aiReply.setCreateTime(LocalDateTime.now());
        aiReply.setUpdateTime(LocalDateTime.now());
        aiReply.setDeleted(false);
        save(aiReply);
    }

    /**
     * 组装评论所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @param currentUserId currentUser 主键，用来定位关联业务数据。
     * @return 评论处理后的文本结果。
     */
    private String buildAiReply(Comment comment, Integer currentUserId) {
        String question = stripAiMention(comment.getContent());
        if (question.trim().isEmpty()) {
            question = "请结合这条评论和当前帖子内容进行回复。";
        }
        AiMessage userMessage = new AiMessage();
        userMessage.setUserId(currentUserId);
        userMessage.setRole("user");
        userMessage.setContent("用户在帖子评论区向 AI 助手提问或回复 AI 助手。"
                + "请先理解当前帖子正文、关联游戏和已有评论，再直接回答用户的问题。"
                + "如果用户问“这篇帖子讲了什么”，请概括帖子主题、关键信息和评论讨论点。"
                + "\n用户评论内容：" + question);
        userMessage.setCreateTime(LocalDateTime.now());
        userMessage.setDeleted(false);
        List<AiMessage> history = new ArrayList<>();
        history.add(userMessage);
        try {
            String context = aiCommunityContextService.buildContext()
                    + aiCommunityContextService.buildPostContext(comment.getPostId());
            return miniMaxAiService.chat(history, context);
        } catch (Exception e) {
            log.warn("AI comment reply failed, commentId: {}", comment.getId(), e);
            return "我刚刚没能连接到模型，暂时无法完整回答这个问题。你可以稍后再 @ 我一次，我会继续结合当前帖子内容认真分析。";
        }
    }

    /**
     * 完成评论中的 shouldAiReply 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param comment comment 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示评论当前状态满足业务判断。
     */
    private boolean shouldAiReply(Comment comment) {
        return Boolean.TRUE.equals(comment.getMentionAi())
                || AI_ASSISTANT_USER_ID == safeInt(comment.getReplyUserId())
                || containsAiMention(comment.getContent());
    }

    /**
     * 完成评论中的 containsAiMention 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示评论当前状态满足业务判断。
     */
    private boolean containsAiMention(String content) {
        return content != null && (content.contains("@AI助手") || content.matches("(?is).*@AI(\\s|$).*"));
    }

    /**
     * 完成评论中的 extractMentionNames 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return 评论列表数据。
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
     * 完成评论中的 stripAiMention 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @return 评论处理后的文本结果。
     */
    private String stripAiMention(String content) {
        return content == null ? "" : content
                .replace("@AI助手", "")
                .replace("@ai助手", "")
                .replace("@AI", "")
                .trim();
    }

    /**
     * 完成评论中的 safeInt 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 评论统计值或主键结果。
     */
    private int safeInt(Integer value) {
        return value == null ? Integer.MIN_VALUE : value;
    }
}
