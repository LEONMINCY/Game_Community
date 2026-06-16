package com.sys.pro.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.sys.pro.common.SqlInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置，注册分页拦截器并统一分页查询行为。
 */
@Configuration
public class MybatisPlusConfig {

    @Value("${app.performance.sql-log-enabled:false}")
    private boolean sqlLogEnabled;

    /**
     * 注册 MyBatis Plus 拦截器，统一挂载 SQL 日志能力。
     * @param sqlSessionFactory sqlSessionFactory 字段，来源于当前接口入参或内部调用上下文。
     * @return MybatisPlus处理后的文本结果。
     */
    @Bean
    public String myInterceptor(SqlSessionFactory sqlSessionFactory) {

        // mybatis-plus插件
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setDbType(DbType.MYSQL);
        paginationInnerInterceptor.setOverflow(true);
        mybatisPlusInterceptor.addInnerInterceptor(paginationInnerInterceptor);

        // 自定义sql日志插件
        SqlInterceptor sqlInterceptor = new SqlInterceptor(sqlLogEnabled);

        // 将插件添加到SqlSessionFactory工厂
        org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
        configuration.addInterceptor(mybatisPlusInterceptor);
        configuration.addInterceptor(sqlInterceptor);
        return "interceptor";
    }

}
