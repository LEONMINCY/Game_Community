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
 * GameRatingVo 封装游戏评价页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="GameRating对象", description="")
public class GameRatingVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private Integer gameId;

    private Integer userId;

    private Integer rating;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime updateTime;

}
