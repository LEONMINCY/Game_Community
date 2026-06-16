package com.sys.pro.service;

import com.sys.pro.pojo.Report;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.common.PageResult;
import com.sys.pro.vo.ReportVo;

import java.time.LocalDateTime;

/**
 * ReportService 定义举报审核业务能力，供控制器和其他服务组合调用。
 */
public interface ReportService extends IService<Report> {
    /**
     * 完成举报中的 pageReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 举报分页结果，包含当前页数据和总数。
     */

    PageResult<ReportVo> pageReport(ReportVo param);

    /**
     * 统计后台仍需要人工处理的举报数量，包含新举报和用户提交的处罚申诉。
     * @return 待处理举报和待处理申诉的合计数量。
     */
    int countPendingForAdmin();

    /**
     * 读取举报的 ReportDetail 数据，供页面展示或后续业务判断。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 举报在该步骤产出的业务结果。
     */

    ReportVo getReportDetail(Integer id);

    /**
     * 根据登录账号标识查找最近一次有效处罚，供被封禁用户在登录页查看原因。
     * @param identifier 用户名、手机号或邮箱。
     * @return 最近一次处罚详情。
     */
    ReportVo getLatestPunishmentByIdentifier(String identifier);

    /**
     * 被封禁用户在登录页提交处罚申诉，后台举报管理同步展示申诉进度。
     * @param identifier 用户名、手机号或邮箱。
     * @param appealContent 用户填写的申诉内容。
     */
    void appealLatestPunishmentByIdentifier(String identifier, String appealContent);
    /**
     * 完成举报中的 createReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void createReport(Report report, Integer userId);
    /**
     * 完成举报中的 muteReportedUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param days days 字段，来源于当前接口入参或内部调用上下文。
     */

    void muteReportedUser(Integer reportId, Integer days);
    /**
     * 完成举报中的 unmuteReportedUser 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     */

    void unmuteReportedUser(Integer reportId);
    /**
     * 完成举报中的 updateReportedUserStatus 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param enableFlag enableFlag 字段，来源于当前接口入参或内部调用上下文。
     * @param banDays banDays 字段，来源于当前接口入参或内部调用上下文。
     */

    void updateReportedUserStatus(Integer reportId, Boolean enableFlag, Integer banDays);
    /**
     * 完成举报中的 rejectReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param reason reason 字段，来源于当前接口入参或内部调用上下文。
     */

    void rejectReport(Integer reportId, String reason);
    /**
     * 完成举报中的 appealReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param appealContent appealContent 字段，来源于当前接口入参或内部调用上下文。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     */

    void appealReport(Integer reportId, String appealContent, Integer userId);
    /**
     * 审核举报处罚申诉，回写通过或驳回结果。
     * @param reportId report 主键，用来定位关联业务数据。
     * @param appealStatus appealStatus 字段，来源于当前接口入参或内部调用上下文。
     * @param appealReply appealReply 字段，来源于当前接口入参或内部调用上下文。
     */

    void reviewAppeal(Integer reportId, String appealStatus, String appealReply);

    /**
     * 完成举报中的 deleteReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     */
    void deleteReport(Long id);

    /**
     * 完成举报中的 updateReport 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     */
    void updateReport(Report report);
    /**
     * 读取举报的 MuteEndTime 数据，供页面展示或后续业务判断。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 举报在该步骤产出的业务结果。
     */

    LocalDateTime getMuteEndTime(Integer userId);
}
