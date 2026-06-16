package com.zzy.generate.utils;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 包相关工具类
 */
public class PackageInfoUtil {

    private static final Map<String, Consumer<Set<String>>> PACKAGE_HANDLER_MAP = new HashMap<>();

    static {
        PACKAGE_HANDLER_MAP.put("LocalDateTime", (list) -> {
            list.add(LocalDateTime.class.getCanonicalName());
            list.add(JsonFormat.class.getCanonicalName());
        });
        PACKAGE_HANDLER_MAP.put("LocalDate", (list) -> {
            list.add(LocalDate.class.getCanonicalName());
            list.add(JsonFormat.class.getCanonicalName());
        });
        PACKAGE_HANDLER_MAP.put("DateTime", (list) -> list.add(Date.class.getCanonicalName()));
        PACKAGE_HANDLER_MAP.put("BigDecimal", (list) -> list.add(BigDecimal.class.getCanonicalName()));
    }

    /**
     * 根据字段获取需要导包的特殊类路径（LocalDateTime、BigDecimal等）
     */
    public static void setFieldPackagePath(String fieldType, Set<String> packagePathList) {
        PACKAGE_HANDLER_MAP.getOrDefault(fieldType, (item) -> {
        }).accept(packagePathList);
    }

}
