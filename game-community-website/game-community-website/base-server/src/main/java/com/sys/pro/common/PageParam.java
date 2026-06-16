package com.sys.pro.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * PageParam 提供分页模型通用能力，统一项目内重复使用的基础模型或拦截逻辑。
 */
@ApiModel("分页参数")
@Data
public class PageParam implements Serializable {

    private static final Integer PAGE_NO = 1;
    private static final Integer PAGE_SIZE = 10;

    @ApiModelProperty(value = "页码，从 1 开始", required = true,example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

    @ApiModelProperty(value = "每页条数，最大值为 100", required = true, example = "10")
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 100, message = "每页条数最大值为 100")
    private Integer pageSize = PAGE_SIZE;

    @ApiModelProperty(value = "拓展字段1")
    private String ex1;

    @ApiModelProperty(value = "拓展字段2")
    private String ex2;

    @ApiModelProperty(value = "拓展字段3")
    private String ex3;

    @ApiModelProperty(value = "拓展字段4")
    private String[] ex4;

    @ApiModelProperty(value = "拓展字段5")
    private String ex5;

    @ApiModelProperty(value = "用户id")
    private Integer userId;
}
