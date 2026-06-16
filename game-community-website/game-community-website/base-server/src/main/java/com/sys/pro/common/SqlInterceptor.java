package com.sys.pro.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j

@Intercepts({

        //type指定代理的是那个对象，method指定代理Executor中的那个方法,args指定Executor中的query方法都有哪些参数对象

        //由于Executor中有两个query，因此需要两个@Signature

        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),//需要代理的对象和方法

        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),

        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})//需要代理的对象和方法

})
/**
 * SQL 执行拦截器，在开发调试时输出 SQL、参数和查询结果。
 */
public class SqlInterceptor implements Interceptor {

    /**
     * SQL 日志默认关闭，避免高并发下频繁反射解析参数和输出大结果集拖慢请求。
     */
    private final boolean sqlLogEnabled;

    /**
     * 创建默认关闭 SQL 日志的拦截器，避免生产环境误输出大结果集。
     */
    public SqlInterceptor() {
        this(false);
    }

    /**
     * 创建可配置开关的 SQL 拦截器，方便开发环境按需开启详细 SQL 日志。
     *
     * @param sqlLogEnabled true 表示输出 SQL、参数和查询结果。
     */
    public SqlInterceptor(boolean sqlLogEnabled) {
        this.sqlLogEnabled = sqlLogEnabled;
    }


    /**
     * 拦截 MyBatis 查询或更新过程，输出 SQL、参数和执行结果。
     * @param invocation MyBatis 调用上下文，包含被拦截的方法和参数。
     * @return SQL 执行日志在该步骤产出的业务结果。
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!sqlLogEnabled || !log.isInfoEnabled()) {
            return invocation.proceed();
        }

        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);

        // 获取参数映射列表
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

        // 获取参数对象
        Object parameterObject = boundSql.getParameterObject();

        // 存储参数值
        List<Object> values = new ArrayList<>();

        // 遍历参数映射列表，获取每个占位符上对应的参数
        for (ParameterMapping parameterMapping : parameterMappings) {
            String property = parameterMapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(property)) {
                // 如果参数被动态添加了，则通过additionalParameters获取值
                value = boundSql.getAdditionalParameter(property);
            } else if (parameterObject == null) {
                value = null;
            } else if (mappedStatement.getConfiguration().getTypeHandlerRegistry().hasTypeHandler(parameterObject.getClass())) {
                // 如果存在对应的TypeHandler，则通过TypeHandler解析参数值
                value = parameterObject;
            } else {
                MetaObject metaObject = mappedStatement.getConfiguration().newMetaObject(parameterObject);
                value = metaObject.getValue(property);
            }

            values.add(value);
        }

        Object result = invocation.proceed();
        log.info("SQL：{}, 参数：{}，结果：{} ",  boundSql.getSql().replaceAll("\\s+", " "), values, result);
        return result;
    }

    /**
     * 为 MyBatis 目标对象挂载当前拦截器。
     * @param target MyBatis 被增强对象，插件会在该对象上挂载拦截器。
     * @return SQL 执行日志在该步骤产出的业务结果。
     */
    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    /**
     * 接收 MyBatis 拦截器配置属性，当前实现保留扩展入口。
     * @param properties MyBatis 传入的拦截器属性，预留给后续扩展。
     */
    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }

}
