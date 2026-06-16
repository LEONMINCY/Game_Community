package com.sys.pro.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * PostPageVo 封装社区帖子页面响应数据，按前端展示需要组织字段。
 */
@Data
@ApiModel(description = "帖子分页查询返回对象")
public class PostPageVo {
    
    @ApiModelProperty(value = "帖子ID")
    private Integer id;
    
    @ApiModelProperty(value = "帖子标题")
    private String title;
    
    @ApiModelProperty(value = "帖子内容")
    private String content;
    
    @ApiModelProperty(value = "帖子媒体内容")
    private String media;

    @ApiModelProperty(value = "话题标签，逗号分隔")
    private String topics;
    
    @ApiModelProperty(value = "游戏ID")
    private Integer gameId;
    
    @ApiModelProperty(value = "游戏名称")
    private String gameName;
    
    @ApiModelProperty(value = "游戏图标")
    private String gameIcon;
    
    @ApiModelProperty(value = "点赞数")
    private Integer likes;
    
    @ApiModelProperty(value = "收藏数")
    private Integer favorites;

    @ApiModelProperty(value = "转发次数")
    private Integer shareCount;
    
    @ApiModelProperty(value = "发帖时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "审核状态")
    private String auditStatus;

    @ApiModelProperty(value = "审核拒绝原因")
    private String auditReason;

    @ApiModelProperty(value = "申诉内容")
    private String appealContent;

    @ApiModelProperty(value = "申诉状态")
    private String appealStatus;

    @ApiModelProperty(value = "申诉处理回复")
    private String appealReply;

    @ApiModelProperty(value = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "申诉时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime appealTime;
    
    @ApiModelProperty(value = "用户昵称")
    private String nickname;
    
    @ApiModelProperty(value = "用户头像")
    private String userAvatar;
    
    @ApiModelProperty(value = "用户等级")
    private Integer userLevel;
} 
