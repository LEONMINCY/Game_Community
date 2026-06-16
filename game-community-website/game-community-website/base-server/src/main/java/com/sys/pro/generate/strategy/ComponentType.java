package com.sys.pro.generate.strategy;

import com.sys.pro.common.ApplicationContextProvider;
import com.sys.pro.generate.pojo.BaseInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.annotation.PostConstruct;
/**
 * 代码生成组件类型枚举，描述表单字段可使用的前端控件类型。
 */
public enum ComponentType {



    DIALOG {



        /**
         * 完成ComponentType中的 generate 步骤，保证该环节的数据和状态可以继续向下流转。
         * @param dialogFieldInfos dialogFieldInfos 字段，来源于当前接口入参或内部调用上下文。
         * @return ComponentType处理后的文本结果。
         */
        @Override
        public String generate(BaseInfo dialogFieldInfos) {
            DialogGenerationStrategy strategy = ApplicationContextProvider.getBean(DialogGenerationStrategy.class);
            return strategy.generateComponent(dialogFieldInfos);
        }
    };

    /**
     * 完成ComponentType中的 generate 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param baseInfo baseInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return ComponentType在该步骤产出的业务结果。
     */
    public abstract String generate(BaseInfo baseInfo);
}
