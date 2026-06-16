package com.sys.pro.service;

import com.sys.pro.dto.UploadedFileResource;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 为上传图片生成和输出缩略图，减少社区列表、榜单和聊天记录中的大图加载压力。
 */
public interface ThumbnailService {

    /**
     * 定位指定宽度的缩略图资源；不存在时由实现层按原图生成。
     *
     * @param fileName 原图文件名或上传文件访问路径。
     * @param width 目标缩略图宽度，控制列表页图片体积。
     * @return 可写入 HTTP 响应的缩略图资源。
     */
    UploadedFileResource resolveThumbnail(String fileName, int width) throws IOException;

    /**
     * 将缩略图写入响应流，供前端 img 标签直接加载。
     *
     * @param fileName 原图文件名或上传文件访问路径。
     * @param width 目标缩略图宽度，控制输出图片尺寸。
     * @param response HTTP 响应对象，用来写出图片内容和缓存头。
     */
    void writeThumbnail(String fileName, int width, HttpServletResponse response) throws IOException;
}
