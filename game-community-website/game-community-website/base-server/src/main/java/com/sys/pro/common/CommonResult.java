package com.sys.pro.common;

import lombok.Data;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * 接口统一响应体，约定成功标识、提示消息和业务数据的返回格式。
 */
@Data
public class CommonResult<T> implements Serializable {



    private Integer code;

    private String msg;

    private T data;



    /**
     * 构造失败响应体，统一前端处理错误码和错误提示。
     * @param code 业务错误码，用来区分不同失败原因。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    public static <T> CommonResult<T> error(Integer code, String message) {
        Assert.isTrue(!GlobalErrorCodeConstants.SUCCESS.getCode().equals(code), "code 必须是错误的！");
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.msg = message;
        return result;
    }

    /**
     * 构造失败响应体，统一前端处理错误码和错误提示。
     * @param errorCode 系统错误码对象，包含业务码和提示消息。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }

    /**
     * 构造成功响应体，统一前端处理成功提示和返回数据。
     * @param data 响应数据，会被包装到统一返回结构中。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = GlobalErrorCodeConstants.SUCCESS.getCode();
        result.data = data;
        result.msg = "请求成功";
        return result;
    }

    /**
     * 构造成功响应体，统一前端处理成功提示和返回数据。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    public static <T> CommonResult<T> success() {
        CommonResult<T> result = new CommonResult<>();
        result.code = GlobalErrorCodeConstants.SUCCESS.getCode();
        result.msg = "请求成功";
        return result;
    }
}
