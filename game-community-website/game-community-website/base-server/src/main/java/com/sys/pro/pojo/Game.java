package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Game 是游戏资料实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_game")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Game对象", description="游戏表")
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "游戏唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "游戏名称")
    private String name;

    @ApiModelProperty(value = "游戏类型")
    private String type;

    @ApiModelProperty(value = "Supported game platforms")
    private String platforms;

    @ApiModelProperty(value = "游戏icon")
    private String icon;

    @ApiModelProperty(value = "开发商名称")
    private String developer;

    @ApiModelProperty(value = "游戏价格")
    private Integer price;

    @ApiModelProperty(value = "折扣百分比")
    private Integer discount;

    @ApiModelProperty(value = "价格标记：historical_low-史低，tie_historical_low-平史低")
    @TableField("price_mark")
    private String priceMark;

    @ApiModelProperty(value = "发售日期")
    @JsonFormat(pattern="yyyy-MM-dd", timezone="GMT+8")
    @TableField("release_date")
    private LocalDate releaseDate;

    @ApiModelProperty(value = "玩家评分")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "#.00")
    private BigDecimal rating;

    @ApiModelProperty(value = "游戏简介")
    private String description;

    @ApiModelProperty(value = "游戏截图URL（JSON数组）")
    private String screenshots;

    @ApiModelProperty(value = "游戏演示视频URL")
    private String video;

    @ApiModelProperty(value = "系统配置要求")
    private String systemRequirements;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    @TableField(exist = false)
    private Integer postCount;

    @TableField(exist = false)
    private Integer salesCount;

    @TableField(exist = false)
    private BigDecimal goodRate;

    @TableField(exist = false)
    private Integer reviewCount;

    @TableField(exist = false)
    private String reviewType;

    @TableField(exist = false)
    private BigDecimal hotScore;

    @TableField(exist = false)
    private Integer originalPrice;

    @TableField(exist = false)
    private Integer finalPrice;



}
