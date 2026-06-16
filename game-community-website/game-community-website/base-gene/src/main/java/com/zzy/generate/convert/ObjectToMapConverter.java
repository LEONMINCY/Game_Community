package com.zzy.generate.convert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectToMapConverter<T> {


    /**
     * 将单个对象转换为 Map<String, Object>。
     *
     * @param obj 对象
     * @return Map<String, Object>
     * @throws IllegalAccessException 如果无法访问字段
     */
    public static Map<String, Object> objectToMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(obj));
        }
        return map;
    }

    /**
     * 将 List<T> 转换为 List<Map<String, Object>>。
     *
     * @param objects 对象列表
     * @return Map列表
     * @throws IllegalAccessException 如果无法访问字段
     */
    public List<Map<String, Object>> convertListToMap(List<T> objects) throws IllegalAccessException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Object obj : objects) {
            resultList.add(objectToMap(obj));
        }
        return resultList;
    }
}
