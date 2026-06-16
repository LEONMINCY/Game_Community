package com.sys.pro.config;

import com.sys.pro.utils.UploadPathResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Web 层静态资源配置，负责上传文件和访问路径之间的映射。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${server.file.upload-path}")
    private String uploadPath;

    /**
     * 映射上传文件访问路径，让浏览器可以读取本地上传资源。
     * @param registry registry 字段，来源于当前接口入参或内部调用上下文。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = UploadPathResolver.resolvePrimaryUploadDir(uploadPath);
        uploadDir.toFile().mkdirs();
        String[] resourceLocations = UploadPathResolver.resolveReadableUploadDirs(uploadPath).stream()
                .map(path -> path.toUri().toString())
                .toArray(String[]::new);
        registry.addResourceHandler("/noLogin/common/img/**")
                .addResourceLocations(resourceLocations)
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
        registry.addResourceHandler("/files/**", "/uploads/**")
                .addResourceLocations(resourceLocations)
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("http://localhost:81")
//                .allowedMethods("GET", "POST", "PUT", "DELETE")
//                .allowCredentials(true)
//                .maxAge(3600);
//        WebMvcConfigurer.super.addCorsMappings(registry);
//    }
}
