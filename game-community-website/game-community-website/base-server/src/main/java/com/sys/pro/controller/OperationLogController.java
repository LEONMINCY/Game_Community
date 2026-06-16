package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.PageResult;
import com.sys.pro.controller.common.BaseController;
import com.sys.pro.dto.OperationLogQueryDTO;
import com.sys.pro.pojo.OperationLog;
import com.sys.pro.service.OperationLogService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 操作日志接口，负责权限判断和请求转发，查询、导出与清理逻辑交给服务层。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/operation-log")
public class OperationLogController extends BaseController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志数据，返回列表内容和总数信息。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation("分页查询操作日志")
    @PostMapping("/page")
    public CommonResult<PageResult<OperationLog>> page(@RequestBody OperationLogQueryDTO dto) {
        if (!isAdmin()) {
            return CommonResult.error(403, "仅管理员可以查看操作日志");
        }
        return CommonResult.success(operationLogService.pageLogs(dto));
    }

    /**
     * 导出筛选后的数据文件，便于后台留档或排查问题。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    @ApiOperation("导出操作日志")
    @GetMapping("/export")
    public void export(OperationLogQueryDTO dto, HttpServletResponse response) throws IOException {
        if (!isAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("仅管理员可以导出操作日志");
            return;
        }
        operationLogService.writeExportCsv(dto, response);
    }

    /**
     * 清理指定时间范围内的数据记录，减少后台列表干扰。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation("清空指定时间段操作日志")
    @PostMapping("/clear")
    public CommonResult<Integer> clear(@RequestBody OperationLogQueryDTO dto) {
        if (!isAdmin()) {
            return CommonResult.error(403, "仅管理员可以清空操作日志");
        }
        return CommonResult.success(operationLogService.clearRange(dto));
    }

    /**
     * 判断操作日志当前状态是否满足业务条件。
     * @return true 表示操作日志当前状态满足业务判断。
     */
    private boolean isAdmin() {
        return Integer.valueOf(1).equals(getRoleId());
    }
}
