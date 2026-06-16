package com.sys.pro.dto;

import com.sys.pro.common.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * PostPageDTO 封装社区帖子请求参数，避免控制器直接暴露数据库实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "帖子分页查询参数")
public class PostPageDTO extends PageParam {
    
    @ApiModelProperty(value = "帖子标题")
    private String title;

    @ApiModelProperty(value = "搜索关键词")
    private String keyword;

    @ApiModelProperty(value = "话题标签")
    private String topic;
    
    @ApiModelProperty(value = "关联游戏id")
    private Integer gameId;

    @ApiModelProperty(value = "审核状态")
    private String auditStatus;

    @ApiModelProperty(value = "申诉状态")
    private String appealStatus;
    
    @ApiModelProperty(value = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    
    @ApiModelProperty(value = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
} 
