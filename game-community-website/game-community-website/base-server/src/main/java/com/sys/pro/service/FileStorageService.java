package com.sys.pro.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 统一管理站内上传文件的保存、定位和下载输出，避免控制器直接接触磁盘路径。
 */
public interface FileStorageService {

    /**
     * 保存前端上传的文件，并返回可被页面访问的相对地址。
     *
     * @param file 用户提交的 multipart 文件对象。
     * @return 文件访问地址，后续会写入图片、视频或附件字段。
     */
    String upload(MultipartFile file) throws IOException;

    /**
     * 根据 URL 中的文件路径找到真实文件，并限制访问范围只能落在上传目录内。
     *
     * @param path 前端请求中的文件路径片段。
     * @return 本地磁盘上的安全文件路径。
     */
    Path resolveDownloadFile(String path);

    /**
     * 将指定上传文件写入响应流，供浏览器下载或预览。
     *
     * @param path 前端请求中的文件路径片段。
     * @param response HTTP 响应对象，用来写出文件内容和响应头。
     */
    void writeDownload(String path, HttpServletResponse response) throws IOException;
}
