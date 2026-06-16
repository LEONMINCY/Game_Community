package com.sys.pro.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页查询返回结构，封装总数、页码、每页数量和当前页数据。
 */
@ApiModel("分页结果")

@Data
public class PageResult<T> implements Serializable {



    @ApiModelProperty(value = "数据", required = true)

    private List<T> list;



    @ApiModelProperty(value = "总量", required = true)

    private Long total;



    /**
     * 创建空分页对象，供反序列化或手动赋值场景使用。
     */
    public PageResult() {

    }


    /**
     * 根据 MyBatis Plus 分页对象转换成前端统一分页结构。
     *
     * @param page MyBatis Plus 返回的分页结果。
     */
    public PageResult(IPage<T> page) {

        this.list = page.getRecords();

        this.total = page.getTotal();

    }


    /**
     * 使用已有列表和总数构造分页结果，适合自定义 SQL 分页查询。
     *
     * @param list 当前页数据列表。
     * @param total 满足筛选条件的总记录数。
     */
    public PageResult(List<T> list, Long total) {

        this.list = list;

        this.total = total;

    }


    /**
     * 构造只有总数的空分页，常用于无结果或兜底返回。
     *
     * @param total 满足筛选条件的总记录数。
     */
    public PageResult(Long total) {

        this.list = new ArrayList<>();

        this.total = total;

    }


    /**
     * 完成分页结果中的 empty 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 分页结果分页结果，包含当前页数据和总数。
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L);
    }

    /**
     * 完成分页结果中的 empty 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param total total 字段，来源于当前接口入参或内部调用上下文。
     * @return 分页结果分页结果，包含当前页数据和总数。
     */
    public static <T> PageResult<T> empty(Long total) {
        return new PageResult<>(total);
    }

    /**
     * 完成分页结果中的 of 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param page page 字段，来源于当前接口入参或内部调用上下文。
     * @return 分页结果分页结果，包含当前页数据和总数。
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
}
