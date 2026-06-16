package com.sys.pro.generate.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * DialogInfo 是DialogInfo实体，负责承载数据库记录与Java字段之间的映射。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class DialogInfo extends BaseInfo {

    private String model;

    private String title;

    protected List<DialogFieldInfo> fields;

}
