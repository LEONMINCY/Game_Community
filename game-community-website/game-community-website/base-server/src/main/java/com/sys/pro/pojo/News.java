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
 * News 是新闻实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_news")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="News对象", description="社区帖子表")
public class News implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "新闻id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "新闻标题")
    private String title;

    @ApiModelProperty(value = "新闻内容")
    private String content;

    @ApiModelProperty(value = "帖子媒体内容（JSON数组，图片）")
    private String mediaJson;

    @ApiModelProperty(value = "浏览量")
    private Integer browCount;

    private String likesUser;

    private String favoritesUser;

    private Integer shareCount;

    @ApiModelProperty(value = "发帖时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    @TableField(exist = false)
    private Integer likes;

    @TableField(exist = false)
    private Integer favorites;

    @TableField(exist = false)
    private Boolean isLikes;

    @TableField(exist = false)
    private Boolean isFavorites;



}
