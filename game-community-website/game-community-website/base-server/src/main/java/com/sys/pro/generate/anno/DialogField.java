package com.sys.pro.generate.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DialogField 注解用于标记代码生成器需要读取的元数据。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DialogField {

    /**
     * 表单项占位提示，代码生成器会把它写入弹窗输入控件。
     */
    String placeholder() default "";
}
