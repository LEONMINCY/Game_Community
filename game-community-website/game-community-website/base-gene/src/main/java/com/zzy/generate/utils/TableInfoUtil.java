package com.zzy.generate.utils;

import com.zzy.generate.config.SqlConfig;
import com.zzy.generate.info.TableField;
import com.zzy.generate.info.TableInfo;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表信息工具类
 */
@Slf4j
public class TableInfoUtil {

    /**
     * 主键标识
     */
    private static final String PRI = "PRI";

    /**
     * 获取表信息
     * @param tableMap 表信息
     * @param connection 数据库连接
     */
    public static void setTableInfo(Map<Integer, TableInfo> tableMap, Connection connection) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(SqlConfig.TABLE_INFO_SQL);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet != null) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("Name");
                    String tableComment = resultSet.getString("comment");
                    log.info("开始生成表：{}", tableName);
                    TableInfo tableInfo = new TableInfo();

                    // 获取表字段信息
                    tableInfo.setTableFields(TableInfoUtil.getTableFields(connection, tableName));
                    tableInfo.setTableComment(tableComment);
                    if (tableName.startsWith("t_")) {
                        tableName = tableName.substring(2);
                    }
                    tableInfo.setTableName(tableNameToClassName(tableName));

                    tableMap.put(resultSet.getRow(), tableInfo);
                }
            }
        } catch (Exception e) {
            log.error("获取表信息失败", e);
        }
    }

    /**
     * 获取当前表字段信息
     * @param connection 数据库连接
     * @param tableName 表名
     * @return 字段信息
     */
    public static List<TableField> getTableFields(Connection connection, String tableName) {

        try {

            List<TableField> tableFields = new ArrayList<>();
            PreparedStatement preparedStatement = connection.prepareStatement(String.format(SqlConfig.TABLE_FIELD_SQL, tableName));
//            preparedStatement.setObject(1, tableName);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet != null) {
                while (resultSet.next()) {
                    String fieldName = firstCharLowerCase(tableNameToClassName(resultSet.getString("Field")));
                    String fieldType = resultSet.getString("Type");
                    String fieldComment = resultSet.getString("Comment");
                    Boolean keyIdentityFlag = PRI.equalsIgnoreCase(resultSet.getString("Key"));

                    TableField tableField = TableField.builder()
                            .fieldComment(fieldComment)
                            .fieldName(fieldName)
                            .fieldType(fieldType)
                            .keyIdentityFlag(keyIdentityFlag)
                            .build();
                    tableField.setFieldType(fieldType);
                    tableFields.add(tableField);
                }

                return tableFields;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }


    /**
     * 将下划线分隔的字符串转换为驼峰命名的字符串。
     *
     * @param underscoreString 下划线分隔的字符串
     * @return 驼峰命名的字符串
     */
    public static String tableNameToClassName(String underscoreString) {
        StringBuilder camelCaseBuilder = new StringBuilder();
        boolean nextUpperCase = false;

        if (underscoreString.contains("t_")) {
            underscoreString = underscoreString.substring(2);
        }

        for (char c : underscoreString.toCharArray()) {
            if (c == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    camelCaseBuilder.append(Character.toUpperCase(c));
                    nextUpperCase = false;
                } else {
                    camelCaseBuilder.append(camelCaseBuilder.length() == 0 ? Character.toUpperCase(c) : c);
                }
            }
        }

        return camelCaseBuilder.toString();
    }

    /**
     * 将驼峰命名的字符串传唤为-分割的字符串，没有驼峰则返回
     */
    public static String camelCaseToUnderscore(String camelCaseString) {
        StringBuilder underscoreBuilder = new StringBuilder();

        for (int i = 0; i < camelCaseString.length(); i++) {
            char c = camelCaseString.charAt(i);

            if (Character.isUpperCase(c)) {
                underscoreBuilder.append('-');
                underscoreBuilder.append(Character.toLowerCase(c));
            } else {
                underscoreBuilder.append(c);
            }
        }
        return underscoreBuilder.toString();
    }

    /**
     * 首字符大写
     */
    public static String firstCharUpperCase(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 首字符小写
     */
    public static String firstCharLowerCase(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
