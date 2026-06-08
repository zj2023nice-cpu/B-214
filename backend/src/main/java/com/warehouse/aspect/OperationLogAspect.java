package com.warehouse.aspect;

import com.warehouse.annotation.OperationLog;
import com.warehouse.entity.User;
import com.warehouse.repository.UserRepository;
import com.warehouse.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final UserRepository userRepository;

    @Around("@annotation(com.warehouse.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        com.warehouse.entity.OperationLog logEntry = new com.warehouse.entity.OperationLog();
        logEntry.setOperationTime(LocalDateTime.now());
        logEntry.setOperationType(annotation.type().name());
        logEntry.setDescription(annotation.description());
        logEntry.setOperationTarget(annotation.target());
        logEntry.setMethod(joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName());

        String params = buildParams(joinPoint.getArgs(), signature.getParameterNames());
        logEntry.setParams(params.length() > 1000 ? params.substring(0, 1000) : params);

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntry.setIp(getClientIp(request));

            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdHeader);
                    logEntry.setUserId(userId);
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        logEntry.setUsername(user.getUsername());
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        Object result;
        try {
            result = joinPoint.proceed();
            logEntry.setResult("SUCCESS");
        } catch (Throwable e) {
            logEntry.setResult("FAIL");
            String errorMsg = e.getMessage();
            logEntry.setErrorMsg(errorMsg != null && errorMsg.length() > 500
                    ? errorMsg.substring(0, 500) : errorMsg);
            throw e;
        } finally {
            try {
                operationLogService.saveAsync(logEntry);
            } catch (Exception e) {
                log.error("Failed to save operation log: {}", e.getMessage());
            }
        }

        return result;
    }

    private String buildParams(Object[] args, String[] paramNames) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            String value = (args[i] != null) ? args[i].toString() : "null";
            if (value.length() > 200) value = value.substring(0, 200) + "...";
            sb.append(name).append("=").append(value);
        }
        return sb.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
