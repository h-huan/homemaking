package com.hm.module.homemaking.security;

import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.framework.tenant.core.util.TenantUtils;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.hm.module.homemaking.security.AdminScope.denyUnless;

@Service("hmAdmin")
public class HomemakingAdminAccess {
    private final HmRepository repo;
    private final PermissionApi permissions;
    public HomemakingAdminAccess(HmRepository repo, PermissionApi permissions) { this.repo = repo; this.permissions = permissions; }
    public boolean platform() {
        var u = SecurityFrameworkUtils.getLoginUser();
        return u != null && Objects.equals(u.getUserType(), 2) && Objects.equals(u.getTenantId(), 1L)
            && TenantUtils.execute(1L, () -> permissions.hasAnyRoles(u.getId(), "super_admin", "hm_platform"));
    }
    public RoleTemplates.Template template() {
        var u = SecurityFrameworkUtils.getLoginUser();
        denyUnless(u != null && Objects.equals(u.getUserType(), 2));
        if (platform()) return RoleTemplates.get("PLATFORM");
        denyUnless(Objects.equals(u.getTenantId(), repo.tenant()));
        if (permissions.hasAnyRoles(u.getId(), "super_admin")) return RoleTemplates.get("OWNER");
        var rows = repo.jdbc().queryForList("SELECT template_code FROM hm_staff_scope WHERE tenant_id=? AND user_id=?", repo.tenant(), u.getId());
        denyUnless(rows.size() == 1);
        String code = rows.get(0).get("template_code").toString();
        denyUnless(!code.equals("PLATFORM") && permissions.hasAnyRoles(u.getId(), "hm_" + code.toLowerCase(Locale.ROOT)));
        return RoleTemplates.get(code);
    }
    public boolean allowed(String permission) {
        try {
            var t = template();
            var user = SecurityFrameworkUtils.getLoginUser();
            return t.permissions().contains(permission) && TenantUtils.execute(user.getTenantId(), () -> permissions.hasAnyPermissions(user.getId(), permission));
        } catch (org.springframework.security.access.AccessDeniedException e) { return false; }
    }
    public boolean catalog(String kind, String action) {
        return Set.of("stores", "workers", "services").contains(kind) && allowed("homemaking:" + kind + ":" + action);
    }
    public boolean tenant(long id) { return id == repo.tenant() || platform(); }
    public AdminScope scope() {
        var t = template(); long tenant = repo.tenant(); long user = SecurityFrameworkUtils.getLoginUserId();
        var stores = new HashSet<>(repo.jdbc().queryForList("SELECT s.store_id FROM hm_staff_store s JOIN hm_store x ON x.id=s.store_id AND x.tenant_id=s.tenant_id WHERE s.tenant_id=? AND s.user_id=?", Long.class, tenant, user));
        var workers = repo.jdbc().queryForList("SELECT worker_id FROM hm_worker_account WHERE tenant_id=? AND user_id=?", Long.class, tenant, user);
        return new AdminScope(tenant, t.code().equals("PLATFORM"), t.range(), Set.copyOf(stores), workers.isEmpty() ? null : workers.get(0));
    }
    public Map<String, Object> current() {
        var t = template();
        return Map.of("template", t.code(), "range", t.range(), "permissions", t.permissions().stream().filter(this::allowed).sorted().toList(), "storeIds", scope().stores());
    }
}
