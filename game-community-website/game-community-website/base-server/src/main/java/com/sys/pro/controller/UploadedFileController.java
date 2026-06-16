package com.sys.pro.controller;

import com.sys.pro.service.UploadedFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 上传资源读取接口，只负责路由映射，文件定位和响应写入交给服务层。
 */
@RestController
@RequiredArgsConstructor
public class UploadedFileController {

    private final UploadedFileService uploadedFileService;

    @GetMapping({
            "/noLogin/common/img/{fileName:.+}",
            "/files/{fileName:.+}",
            "/uploads/{fileName:.+}"
    })

    /**
     * 读取上传文件资源，兼容旧路径并处理图片缺失兜底。
     * @param fileName fileName 字段，来源于当前接口入参或内部调用上下文。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     */
    public void readUploadedFile(@PathVariable String fileName,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        uploadedFileService.writeResource(fileName, request, response);
    }
}
