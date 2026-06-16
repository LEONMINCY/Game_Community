package com.zzy.generate.info;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 存储表字段信息
 */
@Getter
@Setter
@Builder
public class TableField {

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段类型
     */
    private String fieldType;

    /**
     * 字段注释
     */
    private String fieldComment;

    /**
     * 是否为主键
     */
    private Boolean keyIdentityFlag;

    public void setFieldType(String fieldType) {
        if (fieldType.contains("(")) {
            fieldType = fieldType.substring(0, fieldType.indexOf("("));
        }

        // 根据mysql的数据类型转换为java数据类型
        switch (fieldType.toLowerCase()) {
            case "int":
            case "bigint":
                this.fieldType = "Integer";
                break;
            case "varchar":
            case "char":
            case "text":
                this.fieldType = "String";
                break;
            case "datetime":
            case "timestamp":
                this.fieldType = "LocalDateTime";
                break;
            case "date":
                this.fieldType = "LocalDate";
                break;
            case "decimal":
                this.fieldType = "BigDecimal";
                break;
            case "tinyint":
                this.fieldType = "Boolean";
                break;
            default:
                this.fieldType = "Object";
        }
    }
}
