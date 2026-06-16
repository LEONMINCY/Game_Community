package com.zzy.generate.info;

import lombok.Data;

import java.util.List;

/**
 * 数据表相关信息，存储表名，标注释，存储字段信息
 */
@Data
public class TableInfo {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 字段信息
     */
    private List<TableField> tableFields;
}
