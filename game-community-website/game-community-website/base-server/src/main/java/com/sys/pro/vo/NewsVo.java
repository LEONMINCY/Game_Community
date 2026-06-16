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
 * NewsVo 封装新闻页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="News对象", description="社区帖子表")
public class NewsVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "新闻id")
    private Integer id;

    @ApiModelProperty(value = "关联游戏id")
    private Integer gameId;

    @ApiModelProperty(value = "新闻标题")
    private String title;

    @ApiModelProperty(value = "新闻内容")
    private String content;

    @ApiModelProperty(value = "帖子媒体内容（JSON数组，图片）")
    private String mediaJson;

    @ApiModelProperty(value = "浏览量")
    private Integer browCount;

    @ApiModelProperty(value = "发帖时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

}
