package com.sys.pro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sys.pro.pojo.Report;
import com.sys.pro.vo.ReportVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ReportMapper 是举报审核的数据访问入口，负责MyBatis-Plus基础读写和扩展查询。
 */
public interface ReportMapper extends BaseMapper<Report> {
    /**
     * 完成举报中的 selectReportPage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param page page 字段，来源于当前接口入参或内部调用上下文。
     * @param param 请求参数集合，承载前端提交的筛选或分页字段。
     * @return 举报分页结果，包含当前页数据和总数。
     */

    Page<ReportVo> selectReportPage(Page<ReportVo> page, @Param("param") ReportVo param);
    /**
     * 完成举报中的 selectReportDetail 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param id 记录主键，用来定位本次要处理的数据。
     * @return 举报在该步骤产出的业务结果。
     */

    ReportVo selectReportDetail(@Param("id") Integer id);

    /**
     * 查询指定用户最近一次禁言或封禁处罚，供登录页展示封禁原因和申诉入口。
     * @param userId 被处罚用户主键。
     * @return 最近一次处罚详情。
     */
    ReportVo selectLatestPunishmentByUserId(@Param("userId") Integer userId);
    /**
     * 完成举报中的 selectMuteReportsByUserId 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param userId 用户主键，用来限定当前操作的数据归属。
     * @return 举报列表数据。
     */

    List<Report> selectMuteReportsByUserId(@Param("userId") Integer userId);

    int unmuteActiveReportsByUserId(@Param("userId") Integer userId,
                                    @Param("status") String status,
                                    @Param("reply") String reply,
                                    @Param("updateTime") LocalDateTime updateTime);
}
