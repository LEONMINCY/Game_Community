package com.sys.pro.web;

import com.sys.pro.common.ErrorCode;

/**
 * 业务异常对象，用于把可预期的业务失败转换成统一错误响应。
 */
public class ServiceException extends RuntimeException {

    /**
     * 业务错误码。
     */
    private Integer code;

    /**
     * 返回给前端展示的错误提示。
     */
    private String message;

    /**
     * 保留空构造方法，兼容框架反序列化和异常包装场景。
     */
    public ServiceException() {
    }

    /**
     * 根据统一错误码创建业务异常。
     *
     * @param errorCode 系统预定义错误码。
     */
    public ServiceException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }

    /**
     * 创建带业务错误码和提示文案的异常。
     *
     * @param code    业务错误码。
     * @param message 错误提示。
     */
    public ServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 读取ServiceException的 Code 数据，供页面展示或后续业务判断。
     * @return ServiceException统计值或主键结果。
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 完成ServiceException中的 setCode 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param code 业务错误码，用来区分不同失败原因。
     * @return ServiceException在该步骤产出的业务结果。
     */
    public ServiceException setCode(Integer code) {
        this.code = code;
        return this;
    }

    /**
     * 读取ServiceException的 Message 数据，供页面展示或后续业务判断。
     * @return ServiceException处理后的文本结果。
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 完成ServiceException中的 setMessage 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param message 错误提示文本，会直接返回给前端展示。
     * @return ServiceException在该步骤产出的业务结果。
     */
    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }
}
