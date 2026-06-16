package com.sys.pro.controller;

import com.sys.pro.service.ThumbnailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ThumbnailController 接收前端请求、校验入口参数并调用业务服务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/noLogin/common/thumb")
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    /**
     * 读取缩略图的 Thumbnail 数据，供页面展示或后续业务判断。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @param w w 字段，来源于当前接口入参或内部调用上下文。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    @GetMapping("/{fileName:.+}")
    public void getThumbnail(@PathVariable String fileName,
                             @RequestParam(defaultValue = "420") int w,
                             HttpServletResponse response) throws IOException {
        thumbnailService.writeThumbnail(fileName, w, response);
    }
}
