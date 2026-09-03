package com.hm.framework.security.core.service;

import cn.hutool.core.collection.CollUtil;
import com.hm.framework.common.biz.system.permission.PermissionCommonApi;
import com.hm.framework.security.core.LoginUser;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import lombok.AllArgsConstructor;

import java.util.Arrays;

import static com.hm.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static com.hm.framework.security.core.util.SecurityFrameworkUtils.skipPermissionCheck;

/**
 * 默认的 {@link SecurityFrameworkService} 实现类
 *
 * @author 芋道源码
 */
@AllArgsConstructor
public class SecurityFrameworkServiceImpl implements SecurityFrameworkService {

    private final PermissionCommonApi permissionApi;

    @Override public boolean isPlatform() {
        var user=SecurityFrameworkUtils.getLoginUser();
        return user!=null && java.util.Objects.equals(user.getUserType(),2)
            && permissionApi.isPlatformUser(user.getId(),user.getTenantId());
    }
    @Override public boolean canVisitTenant(Long tenantId) {
        var user=SecurityFrameworkUtils.getLoginUser();
        return user!=null && java.util.Objects.equals(user.getUserType(),2)
            && permissionApi.canVisitTenant(user.getId(),user.getTenantId(),tenantId);
    }
    @Override public void recordPlatformVisit(long target,String method,String path,String ip,int status) {
        permissionApi.recordPlatformVisit(target,method,path,ip,status);
    }

    @Override
    public boolean hasPermission(String permission) {
        return hasAnyPermissions(permission);
    }

    @Override
    public boolean hasAnyPermissions(String... permissions) {
        // 特殊：跨租户访问
        if (isPlatform()) {
            return true;
        }

        // 权限校验
        Long userId = getLoginUserId();
        if (userId == null) {
            return false;
        }
        String[] tenantPermissions=Arrays.stream(permissions)
            .filter(p -> !com.hm.framework.common.biz.system.permission.PlatformPermissions.reserved(p)).toArray(String[]::new);
        return tenantPermissions.length>0 && permissionApi.hasAnyPermissions(userId, tenantPermissions);
    }

    @Override
    public boolean hasRole(String role) {
        return hasAnyRoles(role);
    }

    @Override
    public boolean hasAnyRoles(String... roles) {
        // 特殊：跨租户访问
        if (isPlatform()) {
            return true;
        }

        // 权限校验
        Long userId = getLoginUserId();
        if (userId == null) {
            return false;
        }
        return permissionApi.hasAnyRoles(userId, roles);
    }

    @Override
    public boolean hasScope(String scope) {
        return hasAnyScopes(scope);
    }

    @Override
    public boolean hasAnyScopes(String... scope) {
        // 特殊：跨租户访问
        if (isPlatform()) {
            return true;
        }

        // 权限校验
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user == null) {
            return false;
        }
        return CollUtil.containsAny(user.getScopes(), Arrays.asList(scope));
    }

}
