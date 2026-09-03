package com.hm.module.system.api.permission;

import com.hm.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.hm.module.system.service.permission.PermissionService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Set;

/**
 * 权限 API 实现类
 *
 * @author 芋道源码
 */
@Service
public class PermissionApiImpl implements PermissionApi {

    @Resource
    private PermissionService permissionService;
    @Resource private com.hm.module.system.service.permission.PlatformAccessService platform;
    @Override public boolean isPlatformUser(Long user,Long tenant) { return platform.isOperator(user,tenant); }
    @Override public boolean canVisitTenant(Long user,Long tenant,Long target) { return platform.canVisit(user,tenant,target); }
    @Override public void recordPlatformVisit(long target,String method,String path,String ip,int status) { platform.visitLog(target,method,path,ip,status); }

    @Override
    public Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds) {
        return permissionService.getUserRoleIdListByRoleId(roleIds);
    }

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        return permissionService.hasAnyPermissions(userId, permissions);
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        return permissionService.hasAnyRoles(userId, roles);
    }

    @Override
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        return permissionService.getDeptDataPermission(userId);
    }

}
