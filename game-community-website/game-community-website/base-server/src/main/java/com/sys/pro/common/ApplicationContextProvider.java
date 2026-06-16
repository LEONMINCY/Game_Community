package com.sys.pro.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * ApplicationContextProvider 提供ApplicationContext通用能力，统一项目内重复使用的基础模型或拦截逻辑。
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {



    private static ApplicationContext context;



    /**
     * 保存 Spring 容器上下文，便于静态工具在特殊场景下获取 Bean。
     * @param applicationContext applicationContext 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * 读取ApplicationContextProvider的 ApplicationContext 数据，供页面展示或后续业务判断。
     * @return ApplicationContextProvider在该步骤产出的业务结果。
     */
    public static ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * 从 Spring 容器中读取 Bean，供无法直接注入的工具场景使用。
     * @param beanClass beanClass 字段，来源于当前接口入参或内部调用上下文。
     * @return ApplicationContextProvider在该步骤产出的业务结果。
     */
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    /**
     * 从 Spring 容器中读取 Bean，供无法直接注入的工具场景使用。
     * @param beanName beanName 字段，来源于当前接口入参或内部调用上下文。
     * @param beanClass beanClass 字段，来源于当前接口入参或内部调用上下文。
     * @return ApplicationContextProvider在该步骤产出的业务结果。
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        return context.getBean(beanName, beanClass);
    }
}
