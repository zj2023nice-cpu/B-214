package com.warehouse.aspect;

import com.warehouse.annotation.RequireRole;
import com.warehouse.entity.User;
import com.warehouse.exception.ForbiddenException;
import com.warehouse.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class RoleAspect {

    private final UserRepository userRepository;

    @Around("@within(com.warehouse.annotation.RequireRole) || @annotation(com.warehouse.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireRole requireRole = getAnnotation(joinPoint);
        if (requireRole == null) {
            return joinPoint.proceed();
        }

        String[] requiredRoles = requireRole.value();
        String userRole = getCurrentUserRole();

        if (userRole == null) {
            throw new ForbiddenException("未提供用户身份信息");
        }

        boolean hasRole = Arrays.asList(requiredRoles).contains(userRole);
        if (!hasRole) {
            throw new ForbiddenException("权限不足，需要角色: " + String.join(", ", requiredRoles));
        }

        return joinPoint.proceed();
    }

    private RequireRole getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireRole methodAnnotation = method.getAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        Class<?> targetClass = joinPoint.getTarget().getClass();
        return targetClass.getAnnotation(RequireRole.class);
    }

    private String getCurrentUserRole() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return null;
        }

        try {
            Long userId = Long.parseLong(userIdHeader);
            User user = userRepository.findById(userId).orElse(null);
            return user != null ? user.getRole() : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
