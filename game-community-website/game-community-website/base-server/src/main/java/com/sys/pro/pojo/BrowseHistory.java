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
 * BrowseHistory 是浏览历史实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@TableName("t_browse_history")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="BrowseHistory对象", description="")
public class BrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "浏览用户id")
    private Integer userId;

    @ApiModelProperty(value = "浏览目标 ID（game 或 news 的唯一标识)")
    private Integer targetId;

    @ApiModelProperty(value = "目标类型（game 或 news）")
    private String targetType;

    @ApiModelProperty(value = "浏览时间")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="GMT+8")
    private LocalDateTime browseTime;

    @ApiModelProperty(value = "是否删除")
    private Boolean deleted;



}
