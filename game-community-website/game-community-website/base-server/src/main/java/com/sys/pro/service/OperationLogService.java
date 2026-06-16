package com.sys.pro.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sys.pro.common.PageResult;
import com.sys.pro.dto.OperationLogQueryDTO;
import com.sys.pro.pojo.OperationLog;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OperationLogService 定义操作日志业务能力，供控制器和其他服务组合调用。
 */
public interface OperationLogService extends IService<OperationLog> {
    /**
     * 完成操作日志中的 pageLogs 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志分页结果，包含当前页数据和总数。
     */

    PageResult<OperationLog> pageLogs(OperationLogQueryDTO dto);
    /**
     * 完成操作日志中的 exportCsv 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志处理后的文本结果。
     */

    String exportCsv(OperationLogQueryDTO dto);

    /**
     * 将操作日志导出为 CSV 响应。
     */
    void writeExportCsv(OperationLogQueryDTO dto, HttpServletResponse response) throws IOException;
    /**
     * 完成操作日志中的 clearRange 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param dto 请求 DTO，封装分页、筛选和排序条件。
     * @return 操作日志统计值或主键结果。
     */

    int clearRange(OperationLogQueryDTO dto);
}
