package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户通知表。
 */
@Data
@TableName("t_notification")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Notification对象", description = "用户通知表")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "通知ID")
    private Long id;

    @ApiModelProperty(value = "接收用户ID")
    private Integer userId;

    @ApiModelProperty(value = "触发用户ID")
    private Integer actorId;

    @ApiModelProperty(value = "通知类型")
    private String type;

    @ApiModelProperty(value = "通知标题")
    private String title;

    @ApiModelProperty(value = "通知内容")
    private String content;

    @ApiModelProperty(value = "目标类型")
    private String targetType;

    @ApiModelProperty(value = "目标ID")
    private Integer targetId;

    @ApiModelProperty(value = "前端跳转地址")
    private String targetUrl;

    @ApiModelProperty(value = "是否已读")
    private Boolean readFlag;

    @TableField(exist = false)
    @ApiModelProperty(value = "触发通知用户昵称")
    private String actorNickname;

    @TableField(exist = false)
    @ApiModelProperty(value = "触发通知用户头像")
    private String actorAvatar;

    @TableField(exist = false)
    @ApiModelProperty(value = "触发通知用户等级")
    private Integer actorLevel;

    @TableField(exist = false)
    @ApiModelProperty(value = "当前用户是否已经关注触发通知的用户")
    private Boolean actorFollowed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "删除标识")
    private Boolean deleted;
}
