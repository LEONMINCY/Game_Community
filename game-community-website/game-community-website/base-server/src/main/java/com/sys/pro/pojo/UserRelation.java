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
 * UserRelation 是关注粉丝关系实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_user_relation")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="UserRelation对象", description="用户关系表")
public class UserRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "关系唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "用户ID")
    private Integer userId;

    @ApiModelProperty(value = "关注用户ID")
    private Integer followId;

    @ApiModelProperty(value = "关注时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "删除标识（0-正常，1-删除）")
    private Boolean deleted;



}
