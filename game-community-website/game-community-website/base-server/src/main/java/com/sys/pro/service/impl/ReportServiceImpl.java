package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sys.pro.common.PageResult;
import com.sys.pro.mapper.CommentMapper;
import com.sys.pro.mapper.NewsCommentMapper;
import com.sys.pro.mapper.ReportMapper;
import com.sys.pro.pojo.Comment;
import com.sys.pro.pojo.NewsComment;
import com.sys.pro.pojo.Report;
import com.sys.pro.pojo.User;
import com.sys.pro.repository.UserAccountRepository;
import com.sys.pro.service.NotificationService;
import com.sys.pro.service.ReportService;
import com.sys.pro.service.UserService;
import com.sys.pro.vo.ReportVo;
import com.sys.pro.web.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
/**
 * 举报服务实现，处理评论、新闻评论和用户举报的审核、处罚、申诉与通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    private static final String REPORT_TYPE_COMMENT = "COMMENT";
    private static final String REPORT_TYPE_NEWS_COMMENT = "NEWS_COMMENT";
    private static final String REPORT_TYPE_USER = "USER";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_BANNED = "BANNED";
    private static final String STATUS_UNBANNED = "UNBANNED";
    private static final String STATUS_UNMUTED = "UNMUTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String APPEAL_PENDING = "PENDING";
    private static final String MUTED_PREFIX = "MUTE:";
    private static final String BANNED_PREFIX = "BAN:";
    private static final int AI_ASSISTANT_USER_ID = -100;
    private static final DateTimeFormatter ACTION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportMapper reportMapper;
    private final CommentMapper commentMapper;
    private final NewsCommentMapper newsCommentMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final UserAccountRepository userAccountRepository;

    /**
     * 完成举报中的 pageReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 举报分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<ReportVo> pageReport(ReportVo param) {
        Page<ReportVo> page = new Page<>(param.getPageNo(), param.getPageSize());
        Page<ReportVo> result = reportMapper.selectReportPage(page, param);
        result.getRecords().forEach(this::fillActionTimeInfo);
        return PageResult.of(result);
    }

    /**
     * 统计后台待处理举报提醒数，新举报和处罚申诉都会进入同一个提醒入口。
     * @return 待处理记录数量。
     */
    @Override
    public int countPendingForAdmin() {
        return count(new LambdaQueryWrapper<Report>()
                .eq(Report::getDeleted, false)
                .and(wrapper -> wrapper
                        .eq(Report::getStatus, STATUS_PENDING)
                        .or()
                        .isNull(Report::getStatus)
                        .or()
                        .eq(Report::getAppealStatus, APPEAL_PENDING)));
    }

    /**
     * 读取举报的 ReportDetail 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 举报在该步骤产出的业务结果。
     */
    @Override
    public ReportVo getReportDetail(Integer id) {
        ReportVo detail = reportMapper.selectReportDetail(id);
        if (detail == null) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(404, "举报信息不存在");
        }
        fillActionTimeInfo(detail);
        return detail;
    }

    /**
     * 根据登录页输入的账号标识读取最近一次处罚，便于封禁用户确认原因。
     * @param identifier 用户名、手机号或邮箱。
     * @return 最近一次处罚详情。
     */
    @Override
    public ReportVo getLatestPunishmentByIdentifier(String identifier) {
        User user = findAccountByIdentifier(identifier);
        ReportVo detail = reportMapper.selectLatestPunishmentByUserId(user.getId());
        if (detail == null) {
            throw new ServiceException(404, "未找到账号处罚记录");
        }
        fillActionTimeInfo(detail);
        return detail;
    }

    /**
     * 封禁用户无法登录时，从登录页直接提交最近一次处罚的申诉内容。
     * @param identifier 用户名、手机号或邮箱。
     * @param appealContent 用户填写的申诉说明。
     */
    @Override
    public void appealLatestPunishmentByIdentifier(String identifier, String appealContent) {
        User user = findAccountByIdentifier(identifier);
        ReportVo detail = reportMapper.selectLatestPunishmentByUserId(user.getId());
        if (detail != null) {
            appealReport(detail.getId(), appealContent, user.getId());
            return;
        }
        if (appealContent == null || appealContent.trim().isEmpty()) {
            throw new ServiceException(500, "申诉内容不能为空");
        }

        Report report = new Report();
        report.setUserId(user.getId());
        report.setReportedId(user.getId());
        report.setReportType(REPORT_TYPE_USER);
        report.setReason("账号登录被限制");
        report.setStatus(STATUS_BANNED);
        report.setReply("账号当前禁止登录，用户已从登录页提交申诉");
        report.setAppealContent(appealContent.trim());
        report.setAppealStatus(APPEAL_PENDING);
        report.setAppealTime(LocalDateTime.now());
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        report.setDeleted(false);
        save(report);
    }

    /**
     * 完成举报中的 createReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void createReport(Report report, Integer userId) {
        if (report.getReportedId() == null) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "举报对象不能为空");
        }
        if (report.getReportType() == null || report.getReportType().trim().isEmpty()) {
            report.setReportType(REPORT_TYPE_COMMENT);
        }
        if (!REPORT_TYPE_COMMENT.equals(report.getReportType())
                && !REPORT_TYPE_NEWS_COMMENT.equals(report.getReportType())
                && !REPORT_TYPE_USER.equals(report.getReportType())) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "举报类型不支持");
        }
        if (REPORT_TYPE_USER.equals(report.getReportType()) && isAiAssistantUser(report.getReportedId())) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "AI助手无需举报");
        }

        Integer reportedUserId = resolveReportedUserId(report, userId);
        if (isAiAssistantUser(reportedUserId)) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "AI助手无需举报");
        }
        if (Objects.equals(reportedUserId, userId)) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, REPORT_TYPE_USER.equals(report.getReportType()) ? "不能举报自己" : "不能举报自己的评论");
        }

        int duplicateCount = count(new LambdaQueryWrapper<Report>()
                .eq(Report::getReportedId, report.getReportedId())
                .eq(Report::getReportType, report.getReportType())
                .eq(Report::getUserId, userId)
                .eq(Report::getDeleted, false)
                .eq(Report::getStatus, STATUS_PENDING));
        if (duplicateCount > 0) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "您已经提交过该举报，请等待管理员处理");
        }

        report.setUserId(userId);
        report.setStatus(STATUS_PENDING);
        report.setCreateTime(LocalDateTime.now());
        report.setDeleted(false);
        save(report);
    }

    /**
     * 完成举报中的 deleteReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    @Override
    public void deleteReport(Long id) {
        removeById(id);
    }

    /**
     * 完成举报中的 updateReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateReport(Report report) {
        updateById(report);
    }

    /**
     * 完成举报中的 muteReportedUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param days days 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void muteReportedUser(Integer reportId, Integer days) {
        if (days == null || days < 1) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "禁言天数必须大于0");
        }
        ReportVo detail = getReportDetail(reportId);
        if (detail.getReportedUserId() == null) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(404, "未找到被举报用户");
        }
        LocalDateTime endTime = LocalDateTime.now().plusDays(days);
        Report report = new Report();
        report.setId(reportId);
        report.setStatus(MUTED_PREFIX + endTime.format(ACTION_TIME_FORMATTER));
        report.setReply("已对被举报用户禁言 " + days + " 天，禁言截止：" + endTime.format(DISPLAY_TIME_FORMATTER));
        report.setUpdateTime(LocalDateTime.now());
        updateById(report);
        notifyReportedUser(detail, "举报处罚通知", buildPunishNotice(detail, report.getReply()));
    }

    /**
     * 完成举报中的 unmuteReportedUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     */
    @Override
    public void unmuteReportedUser(Integer reportId) {
        ReportVo detail = getReportDetail(reportId);
        if (detail.getReportedUserId() == null) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(404, "未找到被举报用户");
        }
        if (detail.getStatus() == null || !detail.getStatus().startsWith(MUTED_PREFIX)) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "当前举报未处于禁言状态");
        }

        reportMapper.unmuteActiveReportsByUserId(
                detail.getReportedUserId(),
                STATUS_UNMUTED,
                "已解除被举报用户禁言",
                LocalDateTime.now()
        );
    }

    /**
     * 完成举报中的 updateReportedUserStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param enableFlag enableFlag 字段，来源于当前接口入参或内部调用上下文。
     * @param banDays banDays 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void updateReportedUserStatus(Integer reportId, Boolean enableFlag, Integer banDays) {
        if (enableFlag == null) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "账号状态不能为空");
        }
        ReportVo detail = getReportDetail(reportId);
        if (detail.getReportedUserId() == null) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(404, "未找到被举报用户");
        }

        Report report = new Report();
        report.setId(reportId);
        report.setUpdateTime(LocalDateTime.now());
        if (enableFlag) {
            updateUserBan(detail.getReportedUserId(), true, null);
            report.setStatus(STATUS_UNBANNED);
            report.setReply("已解封被举报用户账号");
        } else if (banDays != null && banDays > 0) {
            LocalDateTime endTime = LocalDateTime.now().plusDays(banDays);
            updateUserBan(detail.getReportedUserId(), false, endTime);
            report.setStatus(BANNED_PREFIX + endTime.format(ACTION_TIME_FORMATTER));
            report.setReply("已封禁被举报用户账号 " + banDays + " 天，封禁截止：" + endTime.format(DISPLAY_TIME_FORMATTER));
        } else {
            updateUserBan(detail.getReportedUserId(), false, null);
            report.setStatus(STATUS_BANNED);
            report.setReply("已永久封禁被举报用户账号");
        }
        updateById(report);
        if (!Boolean.TRUE.equals(enableFlag)) {
            notifyReportedUser(detail, "举报处罚通知", buildPunishNotice(detail, report.getReply()));
        }
    }

    /**
     * 完成举报中的 rejectReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param reason reason 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void rejectReport(Integer reportId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "拒绝原因不能为空");
        }
        Report report = new Report();
        report.setId(reportId);
        report.setStatus(STATUS_REJECTED);
        report.setReply("已拒绝处理：" + reason.trim());
        report.setUpdateTime(LocalDateTime.now());
        updateById(report);
    }

    /**
     * 完成举报中的 appealReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param appealContent appealContent 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */
    @Override
    public void appealReport(Integer reportId, String appealContent, Integer userId) {
        if (appealContent == null || appealContent.trim().isEmpty()) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "申诉内容不能为空");
        }
        ReportVo detail = getReportDetail(reportId);
        if (!Objects.equals(detail.getReportedUserId(), userId)) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(403, "只能申诉与自己相关的举报处理");
        }
        if (!isPunishedStatus(detail.getStatus())) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "当前举报处理不支持申诉");
        }
        Report report = new Report();
        report.setId(reportId);
        report.setAppealContent(appealContent.trim());
        report.setAppealStatus(APPEAL_PENDING);
        report.setAppealTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        updateById(report);
    }

    /**
     * 审核举报处罚申诉，回写通过或驳回结果。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param appealStatus appealStatus 字段，来源于当前接口入参或内部调用上下文。
     * @param appealReply appealReply 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void reviewAppeal(Integer reportId, String appealStatus, String appealReply) {
        if (appealStatus == null || appealStatus.trim().isEmpty()) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "申诉处理状态不能为空");
        }
        String normalizedStatus = appealStatus.trim().toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "申诉处理状态不正确");
        }
        ReportVo detail = getReportDetail(reportId);
        if (!APPEAL_PENDING.equalsIgnoreCase(String.valueOf(detail.getAppealStatus()))) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "当前申诉不是待处理状态");
        }
        Report report = new Report();
        report.setId(reportId);
        report.setAppealStatus(normalizedStatus);
        report.setAppealReply(appealReply == null ? "" : appealReply.trim());
        report.setAppealReviewTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        updateById(report);

        String statusText = "APPROVED".equals(normalizedStatus) ? "已通过" : "已驳回";
        notifyReportedUser(detail, "举报申诉进度更新", "你的举报处罚申诉" + statusText + "。处理回复：" + safe(report.getAppealReply()));
    }

    /**
     * 读取举报的 MuteEndTime 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 举报在该步骤产出的业务结果。
     */
    @Override
    public LocalDateTime getMuteEndTime(Integer userId) {
        List<Report> reports = reportMapper.selectMuteReportsByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        return reports.stream()
                .map(report -> parseActionEndTime(report.getStatus(), MUTED_PREFIX))
                .filter(Objects::nonNull)
                .filter(time -> time.isAfter(now))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 完成举报中的 resolveReportedUserId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 举报统计值或主键结果。
     */
    private Integer resolveReportedUserId(Report report, Integer userId) {
        if (REPORT_TYPE_COMMENT.equals(report.getReportType())) {
            Comment comment = commentMapper.selectById(report.getReportedId());
            if (comment == null || Boolean.TRUE.equals(comment.getDeleted())) {
                /**
                 * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 举报在该步骤产出的业务结果。
                 */
                throw new ServiceException(404, "被举报评论不存在");
            }
            return comment.getUserId();
        }
        if (REPORT_TYPE_NEWS_COMMENT.equals(report.getReportType())) {
            NewsComment comment = newsCommentMapper.selectById(report.getReportedId());
            if (comment == null || Boolean.TRUE.equals(comment.getDeleted())) {
                /**
                 * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
                 * @return 举报在该步骤产出的业务结果。
                 */
                throw new ServiceException(404, "被举报的新闻评论不存在");
            }
            return comment.getUserId();
        }

        User reportedUser = userService.getById(report.getReportedId());
        if (reportedUser == null || Boolean.TRUE.equals(reportedUser.getDeleted())) {
            /**
             * 完成举报中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 举报在该步骤产出的业务结果。
             */
            throw new ServiceException(404, "被举报用户不存在");
        }
        return reportedUser.getId();
    }

    /**
     * 完成举报中的 fillActionTimeInfo 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     */
    /**
     * 将登录页输入的账号标识解析为真实账号，供封禁申诉入口复用。
     * @param identifier 用户名、手机号或邮箱。
     * @return 匹配到的账号。
     */
    private User findAccountByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new ServiceException(500, "账号不能为空");
        }
        User user = userAccountRepository.findByIdentifier(identifier.trim());
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new ServiceException(404, "账号不存在");
        }
        return user;
    }

    private void fillActionTimeInfo(ReportVo report) {
        LocalDateTime muteEndTime = parseActionEndTime(report.getStatus(), MUTED_PREFIX);
        if (muteEndTime != null) {
            report.setMuteEndTime(muteEndTime.format(DISPLAY_TIME_FORMATTER));
        }
        LocalDateTime banEndTime = parseActionEndTime(report.getStatus(), BANNED_PREFIX);
        if (banEndTime != null) {
            report.setBanEndTime(banEndTime.format(DISPLAY_TIME_FORMATTER));
        }
    }

    /**
     * 完成举报中的 updateUserBan 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @param enableFlag enableFlag 字段，来源于当前接口入参或内部调用上下文。
     * @param banEndTime banEndTime 字段，来源于当前接口入参或内部调用上下文。
     */
    private void updateUserBan(Integer userId, Boolean enableFlag, LocalDateTime banEndTime) {
        User patch = new User();
        patch.setId(userId);
        patch.setEnableFlag(enableFlag);
        patch.setBanEndTime(banEndTime);
        userService.updateById(patch);
    }

    /**
     * 判断举报当前状态是否满足业务条件。
     * @param status status 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示举报当前状态满足业务判断。
     */
    private boolean isPunishedStatus(String status) {
        return status != null
                && (status.startsWith(MUTED_PREFIX)
                || status.startsWith(BANNED_PREFIX)
                || STATUS_BANNED.equals(status));
    }

    /**
     * 判断举报当前状态是否满足业务条件。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return true 表示举报当前状态满足业务判断。
     */
    private boolean isAiAssistantUser(Integer userId) {
        return Objects.equals(userId, AI_ASSISTANT_USER_ID);
    }

    /**
     * 组装举报所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param detail detail 字段，来源于当前接口入参或内部调用上下文。
     * @param actionReply actionReply 字段，来源于当前接口入参或内部调用上下文。
     * @return 举报处理后的文本结果。
     */
    private String buildPunishNotice(ReportVo detail, String actionReply) {
        return "你的账号因举报被处理。举报原因：" + safe(detail.getReason())
                + "。处理结果：" + safe(actionReply)
                + "。如果你认为处理有误，可以在此通知中提交申诉。";
    }

    /**
     * 完成举报中的 notifyReportedUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param detail detail 字段，来源于当前接口入参或内部调用上下文。
     * @param title title 字段，来源于当前接口入参或内部调用上下文。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     */
    private void notifyReportedUser(ReportVo detail, String title, String content) {
        if (detail == null || detail.getReportedUserId() == null) {
            return;
        }
        notificationService.createSystemNotification(
                detail.getReportedUserId(),
                null,
                "REPORT_PUNISH",
                title,
                content,
                "REPORT",
                detail.getId(),
                null);
    }

    /**
     * 把空文本转换成兜底值，避免拼接上下文出现 null。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 举报处理后的文本结果。
     */
    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "无" : value.trim();
    }

    /**
     * 解析举报相关输入，把原始字符串或请求参数转换成业务对象。
     * @param status status 字段，来源于当前接口入参或内部调用上下文。
     * @param prefix prefix 字段，来源于当前接口入参或内部调用上下文。
     * @return 举报在该步骤产出的业务结果。
     */
    private LocalDateTime parseActionEndTime(String status, String prefix) {
        if (status == null || !status.startsWith(prefix)) {
            return null;
        }
        try {
            return LocalDateTime.parse(status.substring(prefix.length()), ACTION_TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse report status: {}", status);
            return null;
        }
    }
}
