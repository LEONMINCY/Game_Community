package com.sys.pro.generate.pojo;

import lombok.Data;
import lombok.ToString;

/**
 * DialogFieldInfo 是DialogFieldInfo实体，负责承载数据库记录与Java字段之间的映射。
 */
@Data
@ToString
public class DialogFieldInfo {
    private String name;        // 字段名
    private String label;       // 表单显示的标签

    /**
     * 完成DialogFieldInfo模块中字段类型对应的业务步骤。
     */
    private String type;        // 字段类型 (input, number, date, datetime, switch, etc.)
    private String optionsApi;
    private String placeholder; // 占位符
}
