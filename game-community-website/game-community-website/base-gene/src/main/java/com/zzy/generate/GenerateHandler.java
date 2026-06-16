package com.zzy.generate;

import com.zzy.generate.build.TemplateBuilder;
import com.zzy.generate.config.DBConfig;
import com.zzy.generate.info.TableInfo;
import com.zzy.generate.utils.TableInfoUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 代码生成处理器，包含核心入口
 */
@Slf4j
public class GenerateHandler {

    /**
     * 数据库连接
     */
    private final Connection connection;

    public GenerateHandler() {
        this.connection = this.initConnection();
    }

    private Connection initConnection() {
        try {
            Class.forName(DBConfig.DRIVER_CLASS_NAME);
            return DriverManager.getConnection(DBConfig.URL, DBConfig.USERNAME, DBConfig.PASSWORD);
        } catch (Exception e) {
            log.error("获取数据库连接失败", e);
        }
        return null;
    }


    /**
     * 处理器入口，大致工作如下：
     * 1、通过控制台获取需要生成的表结构、字段信息
     * 2、生成各表对应的po、service、controller、mapper、serviceImpl等文件
     *
     */
    public void handler() {
        try {
            // 获取已存的所有表名称，放入Map，打印控制台选择
            Map<Integer, TableInfo> tableMap = new HashMap<>();
            TableInfoUtil.setTableInfo(tableMap, connection);

            // 调用控制台，输出map，根据选择的key，获取对应table
            Scanner scanner = new Scanner(System.in);
            System.out.println("请选择需要生成的表key(逗号隔开)：");
            tableMap.forEach((k, v) -> System.out.println("key:" + k + ",表:" + v.getTableName()));
            String key = scanner.next();
            String[] keys = key.split(",");
            for (String k : keys) {
                TableInfo tableInfo = tableMap.get(Integer.valueOf(k));
                // 生成po、service、controller、mapper、serviceImpl文件
                TemplateBuilder templateBuilder = TemplateBuilder.getTemplateBuilder();
                templateBuilder.buildFile(tableInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
