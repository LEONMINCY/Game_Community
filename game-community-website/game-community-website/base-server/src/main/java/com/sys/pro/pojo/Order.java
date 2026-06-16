package com.sys.pro.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Order 是订单支付实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_order")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Order对象", description="游戏订单表")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "游戏ID")
    private Integer gameId;

    @ApiModelProperty(value = "订单号")
    private String orderNo;

    @ApiModelProperty(value = "订单状态")
    private String status;

    private String refundStatus;

    private String refundReason;

    private String refundReply;

    @ApiModelProperty(value = "总价格")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "#.00")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "下单时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime refundApplyTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime refundReviewTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

    @TableField(exist = false)
    private String gameName;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String returnUrl;

}
