package com.nexus.iam.config;

import com.nexus.iam.annotation.RequirePermission;
import com.nexus.iam.entities.User;
import com.nexus.iam.service.impl.PermissionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionServiceImpl permissionService;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new SecurityException("Invalid user principal");
        }

        User user = (User) principal;

        boolean hasPermission;
        if (requirePermission.departmentRequired()) {
            // Check for department-specific permission
            hasPermission = user.getRoles().stream()
                    .anyMatch(role -> permissionService.roleHasPermissionCheck(role,
                            requirePermission.resource(), requirePermission.action()));
        } else {
            // Check for general permission
            hasPermission = permissionService.hasPermissionCheck(user,
                    requirePermission.resource(), requirePermission.action());
        }

        if (!hasPermission) {
            throw new SecurityException("Access Denied: User does not have permission to " +
                    requirePermission.action() + " " + requirePermission.resource());
        }
    }
}
