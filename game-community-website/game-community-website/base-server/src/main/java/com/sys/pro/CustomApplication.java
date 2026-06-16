package com.sys.pro;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 游戏社区后端启动入口，启用 Mapper 扫描并交给 Spring Boot 初始化容器。
 */
@MapperScan("com.sys.pro.mapper")
@SpringBootApplication
public class CustomApplication {

    /**
     * 启动游戏社区后端应用，加载 Spring Boot 配置、数据源和安全认证组件。
     * @param args 启动参数，当前项目通常不直接使用。
     */
    public static void main(String[] args) {
        SpringApplication.run(CustomApplication.class, args);
    }
}
