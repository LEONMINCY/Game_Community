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
 * BrowseHistoryVo 封装浏览历史页面响应数据，按前端展示需要组织字段。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="BrowseHistory对象", description="")
public class BrowseHistoryVo extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "浏览用户id")
    private Integer userId;

    @ApiModelProperty(value = "浏览目标 ID（game 或 news 的唯一标识)")
    private Integer rgetId;

    @ApiModelProperty(value = "目标类型（game 或 news）")
    private String rgetType;

    @ApiModelProperty(value = "浏览时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime browseTime;

    @ApiModelProperty(value = "是否删除")
    private Boolean deleted;

}
