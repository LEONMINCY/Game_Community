package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NewsComment 是新闻评论实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_news_comment")
@EqualsAndHashCode(callSuper = false)
public class NewsComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer newsId;

    private Integer userId;

    private String content;

    private String imageUrl;

    private Integer parentId;

    private Integer replyUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private Boolean deleted;

    @TableField(exist = false)
    private Integer pageNo = 1;

    @TableField(exist = false)
    private Integer pageSize = 10;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String avatar;

    @TableField(exist = false)
    private Integer userLevel;

    @TableField(exist = false)
    private String replyUserNickname;

    @TableField(exist = false)
    private List<NewsComment> children;

    @TableField(exist = false)
    private Integer replyCount;

    @TableField(exist = false)
    private List<Integer> mentionUserIds;

    @TableField(exist = false)
    private Boolean mentionAi;
}
