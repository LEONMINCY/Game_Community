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
 * 帖子实体，对应社区帖子表中的内容、关联游戏、话题、审核和申诉字段。
 */
@Data

@TableName("t_post")

@EqualsAndHashCode(callSuper = false)

@ApiModel(value = "Post对象", description = "社区帖子表")
public class Post implements Serializable {



    private static final long serialVersionUID = 1L;



    @ApiModelProperty(value = "帖子唯一标识")

    @TableId(value = "id", type = IdType.AUTO)

    private Integer id;



    @ApiModelProperty(value = "发帖用户ID")

    private Integer userId;



    @ApiModelProperty(value = "关联游戏id")

    private Integer gameId;



    @ApiModelProperty(value = "帖子标题")

    private String title;



    @ApiModelProperty(value = "帖子内容")

    private String content;



    @ApiModelProperty(value = "帖子媒体内容（JSON数组，图文/视频）")
    private String media;

    @ApiModelProperty(value = "话题标签，逗号分隔")
    private String topics;

    @ApiModelProperty(value = "点赞用户")
    private String likesUser;


    @ApiModelProperty(value = "收藏用户")
    private String favoritesUser;

    @ApiModelProperty(value = "转发次数")
    private Integer shareCount;

    @ApiModelProperty(value = "发帖时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")

    private LocalDateTime createTime;



    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "审核状态：pending-待审核，approved-已通过，rejected-已拒绝")
    private String auditStatus;

    @ApiModelProperty(value = "审核拒绝原因")
    private String auditReason;

    @ApiModelProperty(value = "申诉内容")
    private String appealContent;

    @ApiModelProperty(value = "申诉状态：none-未申诉，pending-申诉中，approved-申诉通过，rejected-申诉拒绝")
    private String appealStatus;

    @ApiModelProperty(value = "申诉处理回复")
    private String appealReply;

    @ApiModelProperty(value = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "申诉时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime appealTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    /**
     * 读取帖子的 LikesUser 数据，供页面展示或后续业务判断。
     * @return 帖子处理后的文本结果。
     */
    public String getLikesUser() {
        if (likesUser == null) {
            return "";
        }
        return likesUser;
    }

    /**
     * 读取帖子的 FavoritesUser 数据，供页面展示或后续业务判断。
     * @return 帖子处理后的文本结果。
     */
    public String getFavoritesUser() {
        if (favoritesUser == null) {
            return "";
        }
        return favoritesUser;
    }
}
