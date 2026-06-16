package com.sys.pro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.common.PageResult;
import com.sys.pro.dto.OperationLogQueryDTO;
import com.sys.pro.mapper.OperationLogMapper;
import com.sys.pro.pojo.OperationLog;
import com.sys.pro.service.OperationLogService;
import com.sys.pro.web.ServiceException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 操作日志业务实现，集中处理日志查询条件、CSV 导出和按时间范围清理。
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    /**
     * 完成操作日志中的 pageLogs 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志分页结果，包含当前页数据和总数。
     */
    @Override
    public PageResult<OperationLog> pageLogs(OperationLogQueryDTO dto) {
        OperationLogQueryDTO query = dto == null ? new OperationLogQueryDTO() : dto;
        IPage<OperationLog> page = page(new Page<>(query.getPageNo(), query.getPageSize()), buildWrapper(query));
        return new PageResult<>(page);
    }

    /**
     * 完成操作日志中的 exportCsv 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志处理后的文本结果。
     */
    @Override
    public String exportCsv(OperationLogQueryDTO dto) {
        OperationLogQueryDTO query = dto == null ? new OperationLogQueryDTO() : dto;
        List<OperationLog> logs = list(buildWrapper(query).last("LIMIT 50000"));
        StringBuilder builder = new StringBuilder();
        builder.append('\ufeff');
        builder.append("时间,用户,角色,操作,接口,结果,耗时(ms),IP,异常\n");
        for (OperationLog log : logs) {
            builder.append(csv(log.getCreatedTime())).append(",")
                    .append(csv(log.getUsername())).append(",")
                    .append(csv(log.getRoleName())).append(",")
                    .append(csv(log.getOperationName())).append(",")
                    .append(csv(log.getRequestMethod() + " " + log.getRequestUri())).append(",")
                    .append(csv(Boolean.TRUE.equals(log.getSuccess()) ? "成功" : "失败")).append(",")
                    .append(csv(log.getCostTime())).append(",")
                    .append(csv(log.getIpAddress())).append(",")
                    .append(csv(log.getErrorMessage())).append("\n");
        }
        return builder.toString();
    }

    /**
     * 完成操作日志中的 writeExportCsv 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    @Override
    public void writeExportCsv(OperationLogQueryDTO dto, HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode("operation-log.csv", StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.getWriter().write(exportCsv(dto));
    }

    /**
     * 完成操作日志中的 clearRange 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志统计值或主键结果。
     */
    @Override
    public int clearRange(OperationLogQueryDTO dto) {
        LocalDateTime start = parseTime(dto == null ? null : dto.getStartTime(), false);
        LocalDateTime end = parseTime(dto == null ? null : dto.getEndTime(), true);
        if (start == null || end == null) {
            /**
             * 完成操作日志中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return 操作日志在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "请选择需要清空的开始和结束时间");
        }
        UpdateWrapper<OperationLog> wrapper = new UpdateWrapper<OperationLog>()
                .eq("deleted", 0)
                .ge("created_time", start)
                .le("created_time", end)
                .set("deleted", 1);
        return getBaseMapper().update(null, wrapper);
    }

    /**
     * 组装操作日志所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志在该步骤产出的业务结果。
     */
    private LambdaQueryWrapper<OperationLog> buildWrapper(OperationLogQueryDTO dto) {
        LocalDateTime start = parseTime(dto.getStartTime(), false);
        LocalDateTime end = parseTime(dto.getEndTime(), true);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getDeleted, false)
                .like(StringUtils.isNotBlank(dto.getUsername()), OperationLog::getUsername, StringUtils.trim(dto.getUsername()))
                .eq(dto.getRoleId() != null, OperationLog::getRoleId, dto.getRoleId())
                .eq(dto.getSuccess() != null, OperationLog::getSuccess, dto.getSuccess())
                .ge(start != null, OperationLog::getCreatedTime, start)
                .le(end != null, OperationLog::getCreatedTime, end)
                .orderByDesc(OperationLog::getCreatedTime);
        if (StringUtils.isNotBlank(dto.getKeyword())) {
            String keyword = StringUtils.trim(dto.getKeyword());
            wrapper.and(w -> w.like(OperationLog::getOperationName, keyword)
                    .or().like(OperationLog::getModuleName, keyword)
                    .or().like(OperationLog::getRequestUri, keyword)
                    .or().like(OperationLog::getRequestParams, keyword));
        }
        return wrapper;
    }

    /**
     * 解析操作日志相关输入，把原始字符串或请求参数转换成业务对象。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param endOfDay endOfDay 字段，来源于当前接口入参或内部调用上下文。
     * @return 操作日志在该步骤产出的业务结果。
     */
    private LocalDateTime parseTime(String value, boolean endOfDay) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() == 10) {
            LocalDate date = LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE);
            return LocalDateTime.of(date, endOfDay ? LocalTime.MAX : LocalTime.MIN);
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        }) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * 完成操作日志中的 csv 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 操作日志处理后的文本结果。
     */
    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
