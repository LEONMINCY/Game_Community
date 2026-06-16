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
 * Message 是站内私信实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_message")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Message对象", description="消息表")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "消息唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "发送者ID")
    private Integer senderId;

    @ApiModelProperty(value = "接收者ID")
    private Integer receiverId;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "发送时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    @ApiModelProperty(value = "是否已读，发送后默认未读，打开会话后标记为已读")
    private Boolean readFlag;



}
