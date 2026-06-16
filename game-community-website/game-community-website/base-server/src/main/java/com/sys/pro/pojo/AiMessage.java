package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * AiMessage 是AI消息实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_ai_message")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AiMessage", description = "AI assistant message")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "Conversation id")
    private Long conversationId;

    @ApiModelProperty(value = "User id")
    private Integer userId;

    @ApiModelProperty(value = "Message role: user or assistant")
    private String role;

    @ApiModelProperty(value = "Message content")
    private String content;

    @ApiModelProperty(value = "Message image urls json")
    private String imageUrls;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private Boolean deleted;
}
