package com.sys.pro.service.impl;

import com.sys.pro.dto.UploadedFileResource;
import com.sys.pro.service.UploadedFileService;
import com.sys.pro.utils.UploadPathResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * 上传文件读取服务。
 * 兼容旧 files/uploads 目录，并把图片缺失兜底逻辑集中在业务层，便于后续替换为对象存储。
 */
@Service
public class UploadedFileServiceImpl implements UploadedFileService {

    @Value("${server.file.upload-path}")
    private String uploadPath;

    @Value("${app.performance.upload-cache-seconds:604800}")
    private long uploadCacheSeconds;

    /**
     * 完成文件上传中的 resolve 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传在该步骤产出的业务结果。
     */
    @Override
    public UploadedFileResource resolve(String fileName) throws IOException {
        String safeName = Paths.get(URLDecoder.decode(fileName, StandardCharsets.UTF_8.name()))
                .getFileName()
                .toString();
        for (Path dir : UploadPathResolver.resolveReadableUploadDirs(uploadPath)) {
            Path file = dir.resolve(safeName).normalize();
            if (Files.isRegularFile(file)) {
                return UploadedFileResource.file(file, detectContentType(file));
            }
        }
        if (isImageName(safeName)) {
            return UploadedFileResource.placeholder(createMissingImage(), safeName);
        }
        return UploadedFileResource.notFound();
    }

    /**
     * 完成文件上传中的 writeResource 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    @Override
    public void writeResource(String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException {
        UploadedFileResource resource = resolve(fileName);
        if (resource.isNotFound()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (resource.isFile()) {
            writeFile(request, response, resource.getFile(), resource.getContentType());
            return;
        }
        writeBytes(response, resource);
    }

    /**
     * 完成文件上传中的 writeFile 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @param contentType contentType 字段，来源于当前接口入参或内部调用上下文。
     */
    private void writeFile(HttpServletRequest request, HttpServletResponse response, Path file, String contentType) throws IOException {
        response.setContentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream");
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("X-Content-Type-Options", "nosniff");
        long fileSize = Files.size(file);
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        String etag = buildEtag(file, fileSize, lastModified);
        writeCacheHeaders(response, etag, lastModified);

        String rangeHeader = request == null ? null : request.getHeader("Range");
        if (!StringUtils.hasText(rangeHeader) && isNotModified(request, etag, lastModified)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        ByteRange range = parseRange(rangeHeader, fileSize);
        if (range != null) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + fileSize);
            response.setContentLengthLong(range.length());
            copyRange(file, response, range);
            return;
        }
        response.setContentLengthLong(fileSize);
        try (OutputStream outputStream = response.getOutputStream()) {
            Files.copy(file, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            if (isClientAbort(e)) {
                return;
            }
            throw e;
        }
    }

    /**
     * 完成文件上传中的 writeCacheHeaders 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param etag etag 字段，来源于当前接口入参或内部调用上下文。
     * @param lastModified lastModified 字段，来源于当前接口入参或内部调用上下文。
     */
    private void writeCacheHeaders(HttpServletResponse response, String etag, long lastModified) {
        long maxAge = Math.max(uploadCacheSeconds, 0);
        String cacheControl = maxAge > 0
                ? "public, max-age=" + maxAge + ", immutable"
                : "no-cache";
        response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
        response.setHeader(HttpHeaders.ETAG, etag);
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModified);
    }

    /**
     * 组装文件上传所需的返回结构，把多处查询结果整理成前端可直接使用的数据。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @param fileSize fileSize 字段，来源于当前接口入参或内部调用上下文。
     * @param lastModified lastModified 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传处理后的文本结果。
     */
    private String buildEtag(Path file, long fileSize, long lastModified) {
        String pathHash = Integer.toHexString(file.toAbsolutePath().normalize().toString().hashCode());
        return "W/\"" + pathHash + "-" + fileSize + "-" + lastModified + "\"";
    }

    /**
     * 判断文件上传当前状态是否满足业务条件。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param etag etag 字段，来源于当前接口入参或内部调用上下文。
     * @param lastModified lastModified 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示文件上传当前状态满足业务判断。
     */
    private boolean isNotModified(HttpServletRequest request, String etag, long lastModified) {
        if (request == null) {
            return false;
        }
        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (StringUtils.hasText(ifNoneMatch) && ifNoneMatch.contains(etag)) {
            return true;
        }
        long ifModifiedSince = getDateHeaderSafely(request, HttpHeaders.IF_MODIFIED_SINCE);
        long normalizedLastModified = lastModified / 1000 * 1000;
        return ifModifiedSince >= 0 && ifModifiedSince >= normalizedLastModified;
    }

    /**
     * 读取文件上传的 DateHeaderSafely 数据，供页面展示或后续业务判断。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param name name 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传统计值或主键结果。
     */
    private long getDateHeaderSafely(HttpServletRequest request, String name) {
        try {
            return request.getDateHeader(name);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /**
     * 完成文件上传中的 copyRange 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param range range 字段，来源于当前接口入参或内部调用上下文。
     */
    private void copyRange(Path file, HttpServletResponse response, ByteRange range) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {
            long skipped = inputStream.skip(range.start);
            while (skipped < range.start) {
                long next = inputStream.skip(range.start - skipped);
                if (next <= 0) {
                    break;
                }
                skipped += next;
            }
            byte[] buffer = new byte[8192];
            long remaining = range.length();
            while (remaining > 0) {
                int read = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) {
                    break;
                }
                outputStream.write(buffer, 0, read);
                remaining -= read;
            }
            outputStream.flush();
        } catch (IOException e) {
            if (isClientAbort(e)) {
                return;
            }
            throw e;
        }
    }

    /**
     * 解析文件上传相关输入，把原始字符串或请求参数转换成业务对象。
     * @param header header 字段，来源于当前接口入参或内部调用上下文。
     * @param fileSize fileSize 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传在该步骤产出的业务结果。
     */
    private ByteRange parseRange(String header, long fileSize) {
        if (!StringUtils.hasText(header) || !header.startsWith("bytes=") || fileSize <= 0) {
            return null;
        }
        String value = header.substring("bytes=".length()).split(",", 2)[0].trim();
        String[] parts = value.split("-", 2);
        try {
            long start;
            long end;
            if (parts[0].isEmpty()) {
                long suffixLength = Long.parseLong(parts[1]);
                start = Math.max(0, fileSize - suffixLength);
                end = fileSize - 1;
            } else {
                start = Long.parseLong(parts[0]);
                end = parts.length > 1 && StringUtils.hasText(parts[1]) ? Long.parseLong(parts[1]) : fileSize - 1;
            }
            if (start < 0 || start >= fileSize || end < start) {
                return null;
            }
            return new ByteRange(start, Math.min(end, fileSize - 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 完成文件上传中的 writeBytes 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param resource resource 字段，来源于当前接口入参或内部调用上下文。
     */
    private void writeBytes(HttpServletResponse response, UploadedFileResource resource) throws IOException {
        response.setContentType(resource.getContentType());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("X-Missing-Upload-File", resource.getMissingFileName());
        response.setContentLength(resource.getContent().length);
        try (OutputStream outputStream = response.getOutputStream()) {
            outputStream.write(resource.getContent());
            outputStream.flush();
        }
    }

    /**
     * 完成文件上传中的 createMissingImage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 文件上传在该步骤产出的业务结果。
     */
    private byte[] createMissingImage() throws IOException {
        int width = 360;
        int height = 220;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 247, 250));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(220, 225, 233));
            graphics.fillRoundRect(128, 54, 104, 78, 16, 16);
            graphics.setColor(new Color(148, 163, 184));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            graphics.drawString("IMAGE", 148, 102);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            graphics.drawString("file missing", 140, 156);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 判断文件上传当前状态是否满足业务条件。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示文件上传当前状态满足业务判断。
     */
    private boolean isImageName(String fileName) {
        String value = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return value.endsWith(".png")
                || value.endsWith(".jpg")
                || value.endsWith(".jpeg")
                || value.endsWith(".gif")
                || value.endsWith(".webp")
                || value.endsWith(".bmp")
                || value.endsWith(".svg")
                || value.endsWith(".avif");
    }

    /**
     * 完成文件上传中的 detectContentType 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传处理后的文本结果。
     */
    private String detectContentType(Path file) throws IOException {
        String value = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (value.endsWith(".png")) {
            return "image/png";
        }
        if (value.endsWith(".jpg") || value.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (value.endsWith(".gif")) {
            return "image/gif";
        }
        if (value.endsWith(".webp")) {
            return "image/webp";
        }
        if (value.endsWith(".avif")) {
            return "image/avif";
        }
        if (value.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (value.endsWith(".mp4")) {
            return "video/mp4";
        }
        if (value.endsWith(".webm")) {
            return "video/webm";
        }
        if (value.endsWith(".mov")) {
            return "video/quicktime";
        }
        return Files.probeContentType(file);
    }

    /**
     * 判断文件上传当前状态是否满足业务条件。
     * @param error error 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示文件上传当前状态满足业务判断。
     */
    private boolean isClientAbort(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (className.contains("ClientAbortException")
                    || (message != null && (message.contains("Connection reset by peer") || message.contains("Broken pipe")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static class ByteRange {
        private final long start;
        private final long end;

        /**
         * 保存浏览器 Range 请求解析后的字节区间。
         *
         * @param start 起始字节位置，包含该位置。
         * @param end 结束字节位置，包含该位置。
         */
        private ByteRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        /**
         * 完成文件上传中的 length 步骤，保证该环节的数据和状态可以继续向下流转。
         * @return 文件上传统计值或主键结果。
         */
        private long length() {
            return end - start + 1;
        }
    }
}
