package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.pojo.Report;
import com.sys.pro.service.ReportService;
import com.sys.pro.vo.ReportVo;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import com.sys.pro.controller.common.BaseController;

/**
 * 举报管理接口，接收评论、新闻评论和用户举报相关的管理操作。
 */
@Slf4j

@RequiredArgsConstructor

@RestController

@RequestMapping("/report")
public class ReportController extends BaseController {



    private final ReportService reportService;



    /**
     * 接收新增举报数据，完成入口校验后交由业务层保存。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "新增数据")
    @PostMapping("/add")
    public CommonResult<Void> create(@RequestBody Report report) {
        reportService.createReport(report, getUserId());
        return CommonResult.success();
    }

    /**
     * 分页查询举报数据，返回列表内容和总数信息。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/page")
    public CommonResult<PageResult<ReportVo>> page(@RequestBody ReportVo param) {
        return CommonResult.success(reportService.pageReport(param));
    }

    /**
     * 统计后台举报管理需要提醒的待处理数量，包含新举报和用户申诉。
     * @return 待处理数量。
     */
    @GetMapping("/pending-count")
    public CommonResult<Integer> pendingCount() {
        return CommonResult.success(reportService.countPendingForAdmin());
    }

    /**
     * 根据举报主键读取详情，供后台弹窗查看举报对象、处理状态和申诉进度。
     * @param id 举报记录主键。
     * @return 举报详情数据。
     */
    @GetMapping("/detail/{id}")
    public CommonResult<ReportVo> detail(@PathVariable("id") Integer id) {
        return CommonResult.success(reportService.getReportDetail(id));
    }

    /**
     * 分页查询当前用户自己的举报数据，用于个人中心列表。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PostMapping("/my/page")
    public CommonResult<PageResult<ReportVo>> myPage(@RequestBody ReportVo param) {
        if (param == null) {
            param = new ReportVo();
        }
        param.setUserId(getUserId());
        return CommonResult.success(reportService.pageReport(param));
    }

    /**
     * 根据举报处理结果禁言被举报用户，并写入处理状态。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/mute")
    public CommonResult<Void> mute(@RequestBody ReportVo param) {
        reportService.muteReportedUser(param.getId(), param.getMuteDays());
        return CommonResult.success();
    }

    /**
     * 解除用户禁言状态，让用户恢复评论和发帖能力。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/unmute")
    public CommonResult<Void> unmute(@RequestBody ReportVo param) {
        reportService.unmuteReportedUser(param.getId());
        return CommonResult.success();
    }

    /**
     * 调整被举报用户的账号状态，支持封禁和解封处理。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/user-status")
    public CommonResult<Void> updateUserStatus(@RequestBody ReportVo param) {
        reportService.updateReportedUserStatus(param.getId(), param.getEnableFlag(), param.getBanDays());
        return CommonResult.success();
    }

    /**
     * 拒绝举报处理请求，并把原因反馈给举报人。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/reject")
    public CommonResult<Void> reject(@RequestBody ReportVo param) {
        reportService.rejectReport(param.getId(), param.getReply());
        return CommonResult.success();
    }

    /**
     * 提交被处罚用户的申诉内容，等待后台重新审核。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/appeal")
    public CommonResult<Void> appeal(@RequestBody ReportVo param) {
        reportService.appealReport(param.getId(), param.getAppealContent(), getUserId());
        return CommonResult.success();
    }

    /**
     * 审核举报处罚申诉，回写通过或驳回结果。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @PutMapping("/appeal/review")
    public CommonResult<Void> reviewAppeal(@RequestBody ReportVo param) {
        reportService.reviewAppeal(param.getId(), param.getAppealStatus(), param.getAppealReply());
        return CommonResult.success();
    }

    /**
     * 按主键删除举报记录，并让业务层同步处理关联状态。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "删除数据")
    @DeleteMapping("/delete/{id}")
    public CommonResult<Void> delete(@PathVariable("id") Long id) {
        reportService.deleteReport(id);
        return CommonResult.success();
    }

    /**
     * 保存举报编辑后的内容，让前台展示和后台管理保持一致。
     * @param report report 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "更新数据")
    @PutMapping("/update")
    public CommonResult<Void> update(@RequestBody Report report) {
        reportService.updateReport(report);
        return CommonResult.success();
    }

}
