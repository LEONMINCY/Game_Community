package com.sys.pro.common;

import com.alibaba.fastjson.JSON;
import com.sys.pro.pojo.OperationLog;
import com.sys.pro.pojo.UserInfo;
import com.sys.pro.service.OperationLogService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * 控制器切面，记录后台和前台接口调用日志，并异步写入操作日志表。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerAspect {

    private static final int MAX_PARAM_LENGTH = 3000;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final OperationLogService operationLogService;
    private final ExecutorService operationLogExecutor = new ThreadPoolExecutor(
            1,
            2,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5000),
            runnable -> {
                Thread thread = new Thread(runnable, "operation-log-writer");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Value("${app.performance.operation-log-async:true}")
    private boolean operationLogAsync;

    @Value("${app.performance.operation-log-info-enabled:false}")
    private boolean operationLogInfoEnabled;

    /**
     * 环绕控制器方法执行，记录接口耗时、用户和操作结果。
     * @param joinPoint joinPoint 字段，来源于当前接口入参或内部调用上下文。
     * @return 控制器访问日志在该步骤产出的业务结果。
     */
    @Around("execution(* com.sys.pro.controller..*.*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Throwable thrown = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            thrown = ex;
            throw ex;
        } finally {
            saveOperationLog(joinPoint, System.currentTimeMillis() - start, thrown);
        }
    }

    /**
     * 保存控制器访问日志业务结果，并维护后续读取需要的一致状态。
     * @param joinPoint joinPoint 字段，来源于当前接口入参或内部调用上下文。
     * @param costTime costTime 字段，来源于当前接口入参或内部调用上下文。
     * @param thrown thrown 字段，来源于当前接口入参或内部调用上下文。
     */
    private void saveOperationLog(ProceedingJoinPoint joinPoint, long costTime, Throwable thrown) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String controllerName = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getName();
            ApiOperation apiOperation = signature.getMethod().getAnnotation(ApiOperation.class);
            HttpServletRequest request = currentRequest();
            UserInfo userInfo = currentUser();

            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(userInfo == null ? null : userInfo.getUserId());
            operationLog.setUsername(resolveUsername(userInfo));
            operationLog.setRoleId(userInfo == null ? null : userInfo.getRoleId());
            operationLog.setRoleName(resolveRoleName(userInfo == null ? null : userInfo.getRoleId()));
            operationLog.setModuleName(controllerName.replace("Controller", ""));
            operationLog.setOperationName(apiOperation == null ? methodName : apiOperation.value());
            operationLog.setControllerName(controllerName);
            operationLog.setMethodName(methodName);
            operationLog.setRequestMethod(request == null ? null : request.getMethod());
            operationLog.setRequestUri(request == null ? null : request.getRequestURI());
            operationLog.setRequestParams(collectParams(joinPoint.getArgs()));
            operationLog.setIpAddress(resolveIp(request));
            operationLog.setSuccess(thrown == null);
            operationLog.setErrorMessage(thrown == null ? null : truncate(thrown.getMessage(), MAX_ERROR_LENGTH));
            operationLog.setCostTime(costTime);
            operationLog.setCreatedTime(LocalDateTime.now());
            operationLog.setDeleted(false);
            persistOperationLog(operationLog, controllerName, methodName, costTime);
        } catch (Exception logError) {
            log.warn("operation log save failed: {}", logError.getMessage());
        }
    }

    /**
     * 异步写入接口操作日志，避免日志落库拖慢正常请求。
     * @param operationLog operationLog 字段，来源于当前接口入参或内部调用上下文。
     * @param controllerName controllerName 字段，来源于当前接口入参或内部调用上下文。
     * @param methodName methodName 字段，来源于当前接口入参或内部调用上下文。
     * @param costTime costTime 字段，来源于当前接口入参或内部调用上下文。
     */
    private void persistOperationLog(OperationLog operationLog, String controllerName, String methodName, long costTime) {
        Runnable writeTask = () -> {
            try {
                operationLogService.save(operationLog);
                if (operationLogInfoEnabled) {
                    log.info("{}#{} cost={}ms user={} success={}", controllerName, methodName, costTime, operationLog.getUsername(), operationLog.getSuccess());
                }
            } catch (Exception e) {
                log.warn("operation log save failed: {}", e.getMessage());
            }
        };
        if (operationLogAsync) {
            operationLogExecutor.execute(writeTask);
            return;
        }
        writeTask.run();
    }

    /**
     * 关闭操作日志线程池，保证应用停止时不遗留后台线程。
     */
    @PreDestroy
    public void shutdown() {
        operationLogExecutor.shutdown();
    }

    /**
     * 读取当前登录用户资料，供日志记录和权限判断补充上下文。
     * @return 控制器访问日志在该步骤产出的业务结果。
     */
    private UserInfo currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserInfo) {
            return (UserInfo) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前 HTTP 请求对象，用于记录访问路径、参数和客户端地址。
     * @return 控制器访问日志在该步骤产出的业务结果。
     */
    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     * 整理接口入参并脱敏敏感字段，写入后台操作日志。
     * @param args 启动参数，当前项目通常不直接使用。
     * @return 控制器访问日志处理后的文本结果。
     */
    private String collectParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || shouldSkipArg(arg)) {
                continue;
            }
            try {
                values.add(maskSensitive(JSON.toJSONString(arg)));
            } catch (Exception ignored) {
                values.add(maskSensitive(String.valueOf(arg)));
            }
        }
        return truncate(String.join("; ", values), MAX_PARAM_LENGTH);
    }

    /**
     * 判断参数是否不适合写入日志，例如请求流、响应流或上传文件。
     * @param arg arg 字段，来源于当前接口入参或内部调用上下文。
     * @return true 表示控制器访问日志当前状态满足业务判断。
     */
    private boolean shouldSkipArg(Object arg) {
        return arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof MultipartFile
                || arg instanceof MultipartFile[];
    }

    /**
     * 对密码、令牌等敏感字段做遮罩处理，避免日志泄露隐私。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @return 控制器访问日志处理后的文本结果。
     */
    private String maskSensitive(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"oldPassword\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"newPassword\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
    }

    /**
     * 解析真实客户端 IP，兼容代理转发头和本地访问场景。
     * @param request 当前 HTTP 请求，用于读取回调参数或客户端信息。
     * @return 控制器访问日志处理后的文本结果。
     */
    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 根据用户资料补齐日志中的操作者名称。
     * @param userInfo userInfo 字段，来源于当前接口入参或内部调用上下文。
     * @return 控制器访问日志处理后的文本结果。
     */
    private String resolveUsername(UserInfo userInfo) {
        if (userInfo == null) {
            return "guest";
        }
        if (userInfo.getUsername() != null && !userInfo.getUsername().trim().isEmpty()) {
            return userInfo.getUsername();
        }
        if (userInfo.getNickname() != null && !userInfo.getNickname().trim().isEmpty()) {
            return userInfo.getNickname();
        }
        return String.valueOf(userInfo.getUserId());
    }

    /**
     * 把角色编号转换成可读角色名称，便于后台查看日志。
     * @param roleId role 主键，用来定位关联业务数据。
     * @return 控制器访问日志处理后的文本结果。
     */
    private String resolveRoleName(Integer roleId) {
        if (roleId == null) {
            return "guest";
        }
        if (roleId == 1) {
            return "admin";
        }
        if (roleId == 3) {
            return "moderator";
        }
        return "user";
    }

    /**
     * 截断过长文本，防止大参数撑爆操作日志字段。
     * @param value value 字段，来源于当前接口入参或内部调用上下文。
     * @param maxLength maxLength 字段，来源于当前接口入参或内部调用上下文。
     * @return 控制器访问日志处理后的文本结果。
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
