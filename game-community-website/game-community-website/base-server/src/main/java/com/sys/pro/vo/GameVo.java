package com.sys.pro.vo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import com.sys.pro.common.PageParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * GameVo 封装游戏资料页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Game对象", description="游戏表")
public class GameVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "游戏唯一标识")
    private Integer id;

    @ApiModelProperty(value = "游戏名称")
    private String name;

    @ApiModelProperty(value = "游戏类型")
    private String type;

    @ApiModelProperty(value = "Supported game platforms")
    private String platforms;

    @ApiModelProperty(value = "开发商名称")
    private String developer;

    @ApiModelProperty(value = "游戏价格")
    private Integer price;

    @ApiModelProperty(value = "折扣百分比")
    private Integer discount;

    @ApiModelProperty(value = "价格标记：historical_low-史低，tie_historical_low-平史低")
    private String priceMark;

    @ApiModelProperty(value = "发售日期")
    @JsonFormat(pattern="yyyy-MM-dd", timezone="GMT+8")
    private LocalDate releaseDate;

    @ApiModelProperty(value = "原价")
    private Integer originalPrice;

    @ApiModelProperty(value = "折后价")
    private Integer finalPrice;

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

    @ApiModelProperty(value = "评分检索范围参数")
    private String ratingRange;

    @ApiModelProperty(value = "评价类型筛选")

    private String reviewType;

    @ApiModelProperty(value = "是否购买")
    private Boolean isBuy;
}
