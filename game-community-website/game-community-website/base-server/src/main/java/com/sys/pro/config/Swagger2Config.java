package com.sys.pro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger 接口文档配置，暴露后台和前台接口的调试入口。
 */
@Configuration

@EnableSwagger2
public class Swagger2Config {

    /**
     * 配置 Swagger 文档扫描范围，便于开发阶段查看接口。
     * @param apiInfo apiInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return 接口文档在该步骤产出的业务结果。
     */
    @Bean
    public Docket docket(ApiInfo apiInfo) {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.sys.pro.controller")).build();
    }

    /**
     * 配置 Swagger 页面展示的标题、描述和版本信息。
     * @return 接口文档在该步骤产出的业务结果。
     */
    @Bean
    public ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("后端接口文档").version("1.0").build();
    }
}
