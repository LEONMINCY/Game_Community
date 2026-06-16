package com.sys.pro.service;

import com.sys.pro.dto.UploadedFileResource;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 负责把上传文件访问路径解析成可输出资源，兼容旧路径并处理缺失图片兜底。
 */
public interface UploadedFileService {

    /**
     * 根据前端传入的文件名定位上传文件；图片缺失时返回占位图，避免页面出现破图。
     *
     * @param fileName 原始文件名或 URL path 片段
     * @return 可输出到 HTTP 响应的资源描述
     */
    UploadedFileResource resolve(String fileName) throws IOException;

    /**
     * 根据请求范围头输出图片、视频或附件，支持浏览器断点加载大视频。
     *
     * @param fileName 原始文件名或 URL path 片段。
     * @param request HTTP 请求对象，用来读取 Range 等访问头。
     * @param response HTTP 响应对象，用来写出资源内容和缓存头。
     */
    void writeResource(String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
