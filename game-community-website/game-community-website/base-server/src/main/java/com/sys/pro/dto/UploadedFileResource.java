package com.sys.pro.dto;

import lombok.Getter;

import java.nio.file.Path;

/**
 * 上传文件解析结果。
 * Controller 只根据该对象写 HTTP 响应，文件查找和占位图生成由 Service 完成。
 */
@Getter
public class UploadedFileResource {

    private final Path file;
    private final byte[] content;
    private final String contentType;
    private final String missingFileName;
    private final boolean notFound;

    /**
     * 统一创建文件资源描述，避免 Controller 分散判断真实文件、占位图和 404。
     *
     * @param file 可直接读取的本地文件路径。
     * @param content 内存中的占位图内容。
     * @param contentType 输出给浏览器的 MIME 类型。
     * @param missingFileName 原始缺失文件名，用于日志和调试。
     * @param notFound true 表示资源不存在且没有占位内容。
     */
    private UploadedFileResource(Path file, byte[] content, String contentType, String missingFileName, boolean notFound) {
        this.file = file;
        this.content = content;
        this.contentType = contentType;
        this.missingFileName = missingFileName;
        this.notFound = notFound;
    }


    /**
     * 完成文件上传中的 file 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @param contentType contentType 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传在该步骤产出的业务结果。
     */
    public static UploadedFileResource file(Path file, String contentType) {
        return new UploadedFileResource(file, null, contentType, null, false);
    }

    /**
     * 完成文件上传中的 placeholder 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param content content 字段，来源于当前接口入参或内部调用上下文。
     * @param missingFileName missingFileName 字段，来源于当前接口入参或内部调用上下文。
     * @return 文件上传在该步骤产出的业务结果。
     */
    public static UploadedFileResource placeholder(byte[] content, String missingFileName) {
        return new UploadedFileResource(null, content, "image/png", missingFileName, false);
    }

    /**
     * 完成文件上传中的 notFound 步骤，保证该环节的数据和状态可以继续向下流转。
     * @return 文件上传在该步骤产出的业务结果。
     */
    public static UploadedFileResource notFound() {
        return new UploadedFileResource(null, null, null, null, true);
    }

    /**
     * 判断文件上传当前状态是否满足业务条件。
     * @return true 表示文件上传当前状态满足业务判断。
     */
    public boolean isFile() {
        return file != null;
    }
}
