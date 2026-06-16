package com.zzy.generate.config;

/**
 * 存储固定的sql信息，获取表结构和字段信息
 */
public class SqlConfig {

    public static final String TABLE_INFO_SQL = "show table status";

    public static final String TABLE_FIELD_SQL = "show full columns from %s";
}
