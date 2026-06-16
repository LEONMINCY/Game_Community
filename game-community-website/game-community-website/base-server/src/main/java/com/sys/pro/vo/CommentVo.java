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
import java.util.List;

/**
 * CommentVo 封装帖子评论页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Comment对象", description="帖子评论表")
public class CommentVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "评论唯一标识")
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

    @ApiModelProperty(value = "评论点赞数")
    private Integer likes;

    @ApiModelProperty(value = "当前登录用户是否已点赞")
    private Boolean isLiked;

    @ApiModelProperty(value = "当前登录用户ID，仅用于查询点赞状态")
    private Integer currentUserId;

    @ApiModelProperty(value = "评论时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    @ApiModelProperty(value = "用户昵称")
    private String nickname;

    @ApiModelProperty(value = "用户头像")
    private String avatar;

    @ApiModelProperty(value = "用户等级")
    private Integer userLevel;

    @ApiModelProperty(value = "父评论ID")
    private Integer parentId;

    @ApiModelProperty(value = "回复的目标用户ID")
    private Integer replyUserId;

    @ApiModelProperty(value = "回复的目标用户昵称")
    private String replyUserNickname;


    @ApiModelProperty(value = "子评论列表")
    private List<CommentVo> children;

    @ApiModelProperty(value = "子评论数量")
    private Integer replyCount;

    @ApiModelProperty(value = "所属帖子标题")
    private String postTitle;

    @ApiModelProperty(value = "所属帖子封面")
    private String postCover;
}
