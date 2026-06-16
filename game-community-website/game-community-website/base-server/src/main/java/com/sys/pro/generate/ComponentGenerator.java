package com.sys.pro.generate;

import com.sys.pro.generate.pojo.BaseInfo;
import com.sys.pro.generate.pojo.DialogFieldInfo;
import com.sys.pro.generate.strategy.ComponentType;
import com.sys.pro.web.ServiceException;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ComponentGenerator 承载ComponentGenerator模块的支撑代码。
 */
@Slf4j
@Component
public class ComponentGenerator {

    /**
     * 完成ComponentGenerator中的 generate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param componentType componentType 字段，来源于当前接口入参或内部调用上下文。
     * @param baseInfo baseInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return ComponentGenerator处理后的文本结果。
     */
    public String generate(ComponentType componentType, BaseInfo baseInfo) {
        // 直接通过枚举实例调用对应的策略方法
        return componentType.generate(baseInfo);
    }

    /**
     * 完成ComponentGenerator中的 extractDialogFields 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param className className 字段，来源于当前接口入参或内部调用上下文。
     * @return ComponentGenerator列表数据。
     */
    public List<DialogFieldInfo> extractDialogFields(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            List<DialogFieldInfo> fields = new ArrayList<>();

            for (Field field : clazz.getDeclaredFields()) {
                ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
                DialogFieldInfo fieldInfo = new DialogFieldInfo();
                fieldInfo.setName(field.getName());
                fieldInfo.setLabel(apiModelProperty != null ? apiModelProperty.value() : field.getName()); // 字段名作为默认标签
                fieldInfo.setPlaceholder("请输入" + (apiModelProperty != null ? apiModelProperty.value() : field.getName()));

                // 根据字段类型设置类型
                Class<?> fieldType = field.getType();
                if (String.class.equals(fieldType)) {
                    fieldInfo.setType("input");
                } else if (Integer.class.equals(fieldType) || int.class.equals(fieldType) ||
                        Double.class.equals(fieldType) || double.class.equals(fieldType)) {
                    fieldInfo.setType("number");
                } else if (LocalDate.class.equals(fieldType)) {
                    fieldInfo.setType("date");
                } else if (LocalDateTime.class.equals(fieldType)) {
                    fieldInfo.setType("datetime");
                } else if (Boolean.class.equals(fieldType) || boolean.class.equals(fieldType)) {
                    fieldInfo.setType("switch");
                } else {
                    fieldInfo.setType("input"); // 默认使用 input
                }

                fields.add(fieldInfo);
            }

            return fields;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            /**
             * 完成ComponentGenerator中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return ComponentGenerator在该步骤产出的业务结果。
             */
            throw new ServiceException(600, "无法提取对话框字段信息");
        }
    }
}
