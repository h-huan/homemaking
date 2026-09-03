package com.hm.module.homemaking.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect @Component @Order(300)
public class AdminScopeAspect {
    private final HomemakingAdminAccess access;
    public AdminScopeAspect(HomemakingAdminAccess access) { this.access = access; }
    @Around("execution(public * com.hm.module.homemaking.controller.admin..*(..))")
    public Object scoped(ProceedingJoinPoint call) throws Throwable {
        var previous = AdminScope.current();
        try { AdminScope.set(access.scope()); return call.proceed(); }
        finally { AdminScope.set(previous); }
    }
}
