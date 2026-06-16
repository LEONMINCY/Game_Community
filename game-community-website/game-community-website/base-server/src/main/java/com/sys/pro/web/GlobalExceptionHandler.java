package com.sys.pro.web;

import com.sys.pro.common.CommonResult;
import com.sys.pro.common.GlobalErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

/**
 * GlobalExceptionHandler 处理Web层GlobalExceptionHandler异常或请求上下文，保证接口返回格式稳定。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 完成GlobalExceptionHandler中的 allExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    public CommonResult<?> allExceptionHandler(HttpServletRequest request, Throwable ex) {
        if (ex instanceof MissingServletRequestParameterException) {
            return missingServletRequestParameterExceptionHandler((MissingServletRequestParameterException) ex);
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return methodArgumentTypeMismatchExceptionHandler((MethodArgumentTypeMismatchException) ex);
        }
        if (ex instanceof MethodArgumentNotValidException) {
            return methodArgumentNotValidExceptionExceptionHandler((MethodArgumentNotValidException) ex);
        }
        if (ex instanceof BindException) {
            return bindExceptionHandler((BindException) ex);
        }
        if (ex instanceof ValidationException) {
            return validationException((ValidationException) ex);
        }
        if (ex instanceof NoHandlerFoundException) {
            return noHandlerFoundExceptionHandler(request, (NoHandlerFoundException) ex);
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return httpRequestMethodNotSupportedExceptionHandler((HttpRequestMethodNotSupportedException) ex);
        }
        if (ex instanceof AccessDeniedException) {
            return accessDeniedExceptionHandler(request, (AccessDeniedException) ex);
        }
        if (ex instanceof RedisConnectionFailureException) {
            return redisConnectionExceptionHandler(request,(RedisConnectionFailureException) ex);
        }
        return defaultExceptionHandler(request, ex);
    }

    /**
     * 完成GlobalExceptionHandler中的 redisConnectionExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param req req 字段，来源于当前接口入参或内部调用上下文。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = RedisConnectionFailureException.class)
    private CommonResult<?> redisConnectionExceptionHandler(HttpServletRequest req,RedisConnectionFailureException ex) {
        log.warn("[redisConnectionExceptionHandler]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), String.format("连接redis失败，请求url -》 %s", req.getRequestURL()));
    }

    /**
     * 完成GlobalExceptionHandler中的 missingServletRequestParameterExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public CommonResult<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException ex) {
        log.warn("[missingServletRequestParameterExceptionHandler]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), String.format("请求参数缺失:%s", ex.getParameterName()));
    }

    /**
     * 完成GlobalExceptionHandler中的 methodArgumentTypeMismatchExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public CommonResult<?> methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException ex) {
        log.warn("[missingServletRequestParameterExceptionHandler]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), String.format("请求参数类型错误:%s", ex.getMessage()));
    }

    /**
     * 完成GlobalExceptionHandler中的 methodArgumentNotValidExceptionExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> methodArgumentNotValidExceptionExceptionHandler(MethodArgumentNotValidException ex) {
        log.warn("[methodArgumentNotValidExceptionExceptionHandler]", ex);
        FieldError fieldError = ex.getBindingResult().getFieldError();
        assert fieldError != null; // 断言，避免告警
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", fieldError.getDefaultMessage()));
    }

    /**
     * 完成GlobalExceptionHandler中的 bindExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(BindException.class)
    public CommonResult<?> bindExceptionHandler(BindException ex) {
        log.warn("[handleBindException]", ex);
        FieldError fieldError = ex.getFieldError();
        assert fieldError != null; // 断言，避免告警
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", fieldError.getDefaultMessage()));
    }

    /**
     * 完成GlobalExceptionHandler中的 constraintViolationExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public CommonResult<?> constraintViolationExceptionHandler(ConstraintViolationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        ConstraintViolation<?> constraintViolation = ex.getConstraintViolations().iterator().next();
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", constraintViolation.getMessage()));
    }

    /**
     * 完成GlobalExceptionHandler中的 validationException 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = ValidationException.class)
    public CommonResult<?> validationException(ValidationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        // 无法拼接明细的错误信息，因为 Dubbo Consumer 抛出 ValidationException 异常时，是直接的字符串信息，且人类不可读
        return CommonResult.error(GlobalErrorCodeConstants.BAD_REQUEST);
    }

    /**
     * 完成GlobalExceptionHandler中的 noHandlerFoundExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param req req 字段，来源于当前接口入参或内部调用上下文。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public CommonResult<?> noHandlerFoundExceptionHandler(HttpServletRequest req, NoHandlerFoundException ex) {
        log.warn("[noHandlerFoundExceptionHandler]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.NOT_FOUND.getCode(), String.format("请求地址不存在:%s", ex.getRequestURL()));
    }

    /**
     * 完成GlobalExceptionHandler中的 httpRequestMethodNotSupportedExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CommonResult<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException ex) {
        log.warn("[httpRequestMethodNotSupportedExceptionHandler]", ex);
        return CommonResult.error(GlobalErrorCodeConstants.METHOD_NOT_ALLOWED.getCode(), String.format("请求方法不正确:%s", ex.getMessage()));
    }

    /**
     * 完成GlobalExceptionHandler中的 accessDeniedExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param req req 字段，来源于当前接口入参或内部调用上下文。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public CommonResult<?> accessDeniedExceptionHandler(HttpServletRequest req, AccessDeniedException ex) {
        log.warn("[accessDeniedExceptionHandler] 无法访问 url({})]",
                req.getRequestURL(), ex);
        return CommonResult.error(GlobalErrorCodeConstants.FORBIDDEN);
    }

    /**
     * 完成GlobalExceptionHandler中的 serviceExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = ServiceException.class)
    public CommonResult<?> serviceExceptionHandler(ServiceException ex) {
        log.info("[serviceExceptionHandler] code={}, message={}", ex.getCode(), ex.getMessage());
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 完成GlobalExceptionHandler中的 defaultExceptionHandler 步骤，保证该环节的数据和状态可以继续向下流转。
     * @param req req 字段，来源于当前接口入参或内部调用上下文。
     * @param ex ex 字段，来源于当前接口入参或内部调用上下文。
     * @return 包装后的接口响应，包含前端需要的状态码、提示和数据。
     */
    @ExceptionHandler(value = Exception.class)
    public CommonResult<?> defaultExceptionHandler(HttpServletRequest req, Throwable ex) {
        log.error("[defaultExceptionHandler]", ex);
        // 返回 ERROR CommonResult`
        return CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getMsg());
    }
}
