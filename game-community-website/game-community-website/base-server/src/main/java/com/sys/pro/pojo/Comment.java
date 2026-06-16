package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Comment 是帖子评论实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_comment")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Comment对象", description="帖子评论表")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "评论唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "所属帖子ID")
    private Integer postId;

    @ApiModelProperty(value = "评论用户ID")
    private Integer userId;

    @ApiModelProperty(value = "评论内容")
    private String content;

    private String imageUrl;

    @ApiModelProperty(value = "点赞用户ID集合，多个用户用英文逗号分隔")
    private String likesUser;

    @ApiModelProperty(value = "评论时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    @ApiModelProperty(value = "父评论ID，如果是一级评论则为null")
    private Integer parentId;

    @ApiModelProperty(value = "回复的目标用户ID，如果是一级评论则为null")
    private Integer replyUserId;

    @TableField(exist = false)
    @ApiModelProperty(value = "@的用户ID列表")
    private List<Integer> mentionUserIds;

    @TableField(exist = false)
    @ApiModelProperty(value = "是否@AI助手")
    private Boolean mentionAi;

}
