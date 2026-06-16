package com.sys.pro.service.impl;

import com.sys.pro.dto.UploadedFileResource;
import com.sys.pro.service.ThumbnailService;
import com.sys.pro.utils.UploadPathResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

/**
 * 缩略图生成服务。
 * 只为可处理的静态图片生成 JPG 缓存，gif/webp 等格式回退原图，避免前端破图。
 */
@Service
public class ThumbnailServiceImpl implements ThumbnailService {

    @Value("${server.file.upload-path}")
    private String uploadPath;

    /**
     * 完成缩略图中的 resolveThumbnail 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @param width width 字段，来源于当前接口入参或内部调用上下文。
     * @return 缩略图在该步骤产出的业务结果。
     */
    @Override
    public UploadedFileResource resolveThumbnail(String fileName, int width) throws IOException {
        String safeName = Paths.get(fileName).getFileName().toString();
        Optional<Path> originalFile = UploadPathResolver.resolveReadableUploadDirs(uploadPath).stream()
                .map(dir -> dir.resolve(safeName).normalize())
                .filter(Files::isRegularFile)
                .findFirst();
        if (!originalFile.isPresent()) {
            return UploadedFileResource.notFound();
        }
        Path original = originalFile.get();
        int targetWidth = Math.max(120, Math.min(width, 960));
        Path thumbDir = original.getParent().resolve("thumb");
        Files.createDirectories(thumbDir);

        Path thumbFile = thumbDir.resolve(stripExtension(safeName) + "_" + targetWidth + ".jpg");
        if (needsRefresh(original, thumbFile) && !createThumbnail(original, thumbFile, targetWidth)) {
            return UploadedFileResource.file(original, Files.probeContentType(original));
        }
        return UploadedFileResource.file(thumbFile, "image/jpeg");
    }

    /**
     * 完成缩略图中的 writeThumbnail 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @param width width 字段，来源于当前接口入参或内部调用上下文。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    @Override
    public void writeThumbnail(String fileName, int width, HttpServletResponse response) throws IOException {
        UploadedFileResource resource = resolveThumbnail(fileName, width);
        if (resource.isNotFound()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        writeFile(response, resource.getFile(), resource.getContentType());
    }

    /**
     * 完成缩略图中的 writeFile 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @param contentType contentType 字段，来源于当前接口入参或内部调用上下文。
     */
    private void writeFile(HttpServletResponse response, Path file, String contentType) throws IOException {
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=604800, immutable");
        response.setContentLengthLong(Files.size(file));
        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(file, outputStream);
            outputStream.flush();
        }
    }

    /**
     * 完成缩略图中的 needsRefresh 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param original original 字段，来源于当前接口入参或内部调用上下文。
     * @param thumbFile thumbFile 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示缩略图当前状态满足业务判断。
     */
    private boolean needsRefresh(Path original, Path thumbFile) throws IOException {
        return !Files.isRegularFile(thumbFile)
                || Files.getLastModifiedTime(thumbFile).toMillis() < Files.getLastModifiedTime(original).toMillis();
    }

    /**
     * 完成缩略图中的 createThumbnail 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param original original 字段，来源于当前接口入参或内部调用上下文。
     * @param thumbFile thumbFile 字段，来源于当前接口入参或内部调用上下文。
     * @param targetWidth targetWidth 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示缩略图当前状态满足业务判断。
     */
    private boolean createThumbnail(Path original, Path thumbFile, int targetWidth) throws IOException {
        String extension = getExtension(original.getFileName().toString());
        if ("gif".equals(extension) || "webp".equals(extension)) {
            return false;
        }

        BufferedImage source = ImageIO.read(original.toFile());
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return false;
        }

        int width = Math.min(targetWidth, source.getWidth());
        int height = Math.max(1, Math.round(source.getHeight() * (width / (float) source.getWidth())));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return ImageIO.write(target, "jpg", thumbFile.toFile());
    }

    /**
     * 完成缩略图中的 stripExtension 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @return 缩略图处理后的文本结果。
     */
    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * 读取缩略图的 Extension 数据，供页面展示或后续业务判断。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @return 缩略图处理后的文本结果。
     */
    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > -1 ? fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
    }
}
