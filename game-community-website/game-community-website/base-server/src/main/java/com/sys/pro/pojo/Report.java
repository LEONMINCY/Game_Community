package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Report 是举报审核实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_report")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Report对象", description="举报表")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "举报唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "被举报对象ID")
    private Integer reportedId;

    @ApiModelProperty(value = "举报类型（POST/COMMENT/USER）")
    @TableField("report_type")
    private String reportType;

    @ApiModelProperty(value = "举报用户ID")
    private Integer userId;

    @ApiModelProperty(value = "举报理由")
    private String reason;

    private String evidenceText;

    private String evidenceImages;

    @ApiModelProperty(value = "举报状态")
    private String status;

    private String reply;

    private String appealContent;

    private String appealStatus;

    private String appealReply;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime appealTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime appealReviewTime;

    @ApiModelProperty(value = "举报时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;



}
