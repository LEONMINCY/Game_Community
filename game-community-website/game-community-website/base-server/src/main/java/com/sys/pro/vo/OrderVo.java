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
 * OrderVo 封装订单支付页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="Order对象", description="游戏订单表")
public class OrderVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单唯一标识")
    private Integer id;

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "游戏ID")
    private Integer gameId;

    private String orderNo;

    private String gameName;

    @ApiModelProperty(value = "订单状态")
    private String status;

    private String refundStatus;

    private String refundReason;

    private String refundReply;

    @ApiModelProperty(value = "总价格")
    private Integer totalPrice;

    @ApiModelProperty(value = "下单时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;

}
