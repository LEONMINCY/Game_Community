package com.sys.pro.common;

import lombok.Data;

/**
 * 错误码对象
 * 全局错误码，占用 [0, 999], 参见 {@link GlobalErrorCodeConstants}
 * 业务异常错误码，占用 [1 000 000 000, +∞)
 * 错误码设计成对象，便于后续扩展国际化文案。
 */
@Data
public class ErrorCode {

    /**
     * 错误码
     */
    private final Integer code;
    /**
     * 错误提示
     */
    private final String msg;


    /**
     * 创建不可变错误码对象，业务层只需要传入错误编号和展示文案。
     *
     * @param code 错误编号，前端会根据它判断失败类型。
     * @param message 错误提示文案，会透传给调用方展示。
     */
    public ErrorCode(Integer code, String message) {
        this.code = code;
        this.msg = message;
    }

}

