package com.sys.pro.service.impl;

import com.sys.pro.service.FileStorageService;
import com.sys.pro.utils.UploadPathResolver;
import com.sys.pro.web.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * 本地上传文件存储实现。
 * 当前返回兼容旧前端的 /noLogin/common/img/{fileName} 地址，后续可替换为 CDN/对象存储。
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${server.file.upload-path}")
    private String uploadPath;

    @Value("${server.file.public-base-url:}")
    private String publicBaseUrl;

    /**
     * 完成FileStorage中的 upload 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @return FileStorage处理后的文本结果。
     */
    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            /**
             * 完成FileStorage中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return FileStorage在该步骤产出的业务结果。
             */
            throw new ServiceException(500, "上传文件不能为空");
        }
        log.info("upload START name:{}, size:{}, contentType:{}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        Path uploadDir = UploadPathResolver.resolvePrimaryUploadDir(uploadPath);
        Files.createDirectories(uploadDir);
        String fileName = generateFileName(Objects.requireNonNull(file.getOriginalFilename()));
        file.transferTo(uploadDir.resolve(fileName).normalize());

        String publicUrl = buildPublicUrl(fileName);
        log.info("upload success url:{}", publicUrl);
        return publicUrl;
    }

    /**
     * 完成FileStorage中的 resolveDownloadFile 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param path path 字段，来源于当前接口入参或内部调用上下文。
     * @return FileStorage在该步骤产出的业务结果。
     */
    @Override
    public Path resolveDownloadFile(String path) {
        Path file = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            /**
             * 完成FileStorage中的 ServiceException 步骤，保证该环节的数据和状态可以继续向下流转。
             * @return FileStorage在该步骤产出的业务结果。
             */
            throw new ServiceException(404, "文件不存在");
        }
        return file;
    }

    /**
     * 完成FileStorage中的 writeDownload 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param path path 字段，来源于当前接口入参或内部调用上下文。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    @Override
    public void writeDownload(String path, HttpServletResponse response) throws IOException {
        log.info("download START path:{}", path);
        Path file = resolveDownloadFile(path);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + file.getFileName());
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file.toFile()));
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        }
        log.info("download END path:{}", path);
    }

    /**
     * 完成FileStorage中的 generateFileName 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param originalName originalName 字段，来源于当前接口入参或内部调用上下文。
     * @return FileStorage处理后的文本结果。
     */
    private String generateFileName(String originalName) {
        String formatNow = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String extension = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        return formatNow + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4)
                + extension;
    }

    /**
     * 组装FileStorage所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @return FileStorage处理后的文本结果。
     */
    private String buildPublicUrl(String fileName) {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "/noLogin/common/img/" + fileName;
        }
        String base = publicBaseUrl.trim();
        return (base.endsWith("/") ? base : base + "/") + fileName;
    }
}
