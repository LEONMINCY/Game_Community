package com.sys.pro.vo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import com.sys.pro.common.PageParam;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * ReportVo 封装举报审核页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Report对象", description="举报表")
public class ReportVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "举报唯一标识")
    private Integer id;

    @ApiModelProperty(value = "被举报对象ID")
    private Integer reportedId;

    @ApiModelProperty(value = "举报类型（POST/COMMENT/USER）")
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

    private String reporterNickname;

    private Integer reportedUserId;

    private String reportedUsername;

    private String reportedNickname;

    private String reportedAvatar;

    private Boolean reportedEnableFlag;

    private Integer postId;

    private String postTitle;

    private Integer newsId;

    private String newsTitle;

    private String commentContent;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime commentCreateTime;

    private Integer muteDays;

    private Integer banDays;

    private String muteEndTime;

    private Boolean enableFlag;

    private String banEndTime;

    private String reportedKeyword;

}
