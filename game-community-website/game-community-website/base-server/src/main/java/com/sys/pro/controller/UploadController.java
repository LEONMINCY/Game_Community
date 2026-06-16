package com.sys.pro.controller;

import com.sys.pro.common.CommonResult;
import com.sys.pro.service.FileStorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.sys.pro.common.CommonResult.success;

/**
 * UploadController 接收前端请求、校验入口参数并调用业务服务。
 */
@RestController
@Api(value = "上传下载接口")
@RequestMapping("/file")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    /**
     * 接收前端上传的图片、视频或附件，并返回可访问地址。
     * @param file file 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ApiOperation(value = "上传文件")
    @PostMapping("/upload")
    public CommonResult<String> uploadFile(MultipartFile file) throws IOException {
        return success(fileStorageService.upload(file));
    }

    /**
     * 按上传路径输出文件内容，供浏览器下载或预览。
     * @param response 当前 HTTP 响应，用于写出文件或回调结果。
     * @param path path 字段，来源于当前接口入参或内部调用上下文。
     */
    @ApiOperation(value = "下载文件")
    @GetMapping("/download")
    public void downloadFile(HttpServletResponse response, String path) throws IOException {
        fileStorageService.writeDownload(path, response);
    }
}
