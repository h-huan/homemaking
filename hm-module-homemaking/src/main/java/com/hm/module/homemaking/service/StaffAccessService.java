package com.hm.module.homemaking.service;

import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.*;
import com.hm.module.system.service.permission.*;
import com.hm.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.hm.module.homemaking.security.AdminScope.denyUnless;
import static com.hm.module.homemaking.dal.HmRepository.check;

@Service
public class StaffAccessService {
    public record Grant(@Min(1) long userId, @NotBlank String templateCode, @NotNull @Size(max=200) Set<@Min(1) Long> storeIds) {}
    private final HmRepository repo;
    private final HomemakingAdminAccess access;
    private final RoleService roles;
    private final PermissionService permissions;
    public StaffAccessService(HmRepository repo, HomemakingAdminAccess access, RoleService roles, PermissionService permissions) {
        this.repo=repo; this.access=access; this.roles=roles; this.permissions=permissions;
    }
    public Object templates() { return RoleTemplates.ALL_TEMPLATES.stream().filter(t -> !t.code().equals("PLATFORM") || access.platform() && repo.tenant()==1).toList(); }
    public Object users() {
        var rows=repo.jdbc().queryForList("SELECT u.id,u.username,u.nickname,u.status,s.template_code,(SELECT COUNT(*) FROM system_user_role ur JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=ur.tenant_id WHERE ur.user_id=u.id AND ur.tenant_id=u.tenant_id AND ur.deleted=FALSE AND r.deleted=FALSE AND r.code='super_admin') AS super_admin FROM system_users u LEFT JOIN hm_staff_scope s ON s.tenant_id=u.tenant_id AND s.user_id=u.id WHERE u.tenant_id=? AND u.deleted=FALSE ORDER BY u.id LIMIT 1000", repo.tenant());
        boolean write=access.allowed("homemaking:staff:write"),platform=access.platform();
        for(var row:rows)row.put("editable",write && HmRepository.number(row,"id")!=SecurityFrameworkUtils.getLoginUserId() && HmRepository.number(row,"super_admin")==0 && (platform || !"PLATFORM".equals(row.get("template_code"))));
        return rows;
    }
    public Object binding(long user) {
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM system_users WHERE id=? AND tenant_id=? AND deleted=FALSE", Long.class, user, repo.tenant())==1, "用户不存在");
        return Map.of("roles", repo.jdbc().queryForList("SELECT template_code FROM hm_staff_scope WHERE tenant_id=? AND user_id=?", repo.tenant(), user),
                "storeIds", repo.jdbc().queryForList("SELECT store_id FROM hm_staff_store WHERE tenant_id=? AND user_id=? ORDER BY store_id", Long.class, repo.tenant(), user));
    }
    @Transactional
    public void grant(Grant g) {
        denyUnless(access.allowed("homemaking:staff:write"));
        var t = RoleTemplates.get(g.templateCode());
        denyUnless(!t.code().equals("PLATFORM") || access.platform() && repo.tenant()==1);
        denyUnless(g.userId()!=SecurityFrameworkUtils.getLoginUserId());
        check(repo.jdbc().queryForList("SELECT id FROM system_users WHERE tenant_id=? AND id=? AND deleted=FALSE AND status=0 FOR UPDATE", repo.tenant(), g.userId()).size()==1, "请选择本租户有效后台账号");
        var previous = permissions.getUserRoleIdListByUserId(g.userId());
        var previousRoles = roles.getRoleList(previous);
        denyUnless(previousRoles.stream().noneMatch(r -> r.getCode().equals("super_admin")));
        denyUnless(access.platform() || previousRoles.stream().noneMatch(r -> r.getCode().equals("hm_platform")));
        if (t.range().equals("STORES")) {
            check(!g.storeIds().isEmpty(), "必须分配至少一个门店");
            for (long id : g.storeIds()) repo.require("hm_store", id, false);
        } else check(g.storeIds().isEmpty(), "本角色不使用门店列表");
        String code = "hm_" + t.code().toLowerCase(Locale.ROOT);
        var existing = roles.getRoleList().stream().filter(r -> r.getCode().equals(code)).findFirst();
        long role;
        if (existing.isPresent()) { role=existing.get().getId(); check(existing.get().getStatus()==0,"模板角色已停用，请先在角色管理启用"); }
        else {
            var request=new RoleSaveReqVO(); request.setCode(code); request.setName(t.name()); request.setSort(100); request.setStatus(0); request.setRemark("家政角色模板；数据范围通过家政人员权限管理");
            role=roles.createRole(request, 2);
        }
        // Reapply the bounded template, using platform services so permission caches are invalidated.
        var menuIds=new HashSet<Long>(); menuIds.add(900000L);
        var menus=repo.jdbc().queryForList("SELECT id,permission FROM system_menu WHERE deleted=FALSE AND status=0 AND id BETWEEN 900001 AND 901999");
        for(var menu:menus) if(t.permissions().contains(Objects.toString(menu.get("permission"),""))) menuIds.add(HmRepository.number(menu,"id"));
        if(t.permissions().stream().anyMatch(p -> p.matches("homemaking:(stores|workers|services|orders|customers|aftersales|reviews):.*"))) menuIds.add(900001L);
        if(t.permissions().contains("homemaking:brand:read")) menuIds.add(900002L);
        if(t.code().equals("PLATFORM")) menuIds.addAll(repo.jdbc().queryForList("SELECT id FROM system_menu WHERE permission='system:tenant:visit' AND deleted=FALSE AND status=0",Long.class));
        permissions.assignRoleMenu(role,menuIds);
        var next=new HashSet<Long>();
        for(var r:previousRoles) if(!r.getCode().startsWith("hm_")) next.add(r.getId());
        next.add(role); permissions.assignUserRole(g.userId(),next);
        repo.jdbc().update("INSERT INTO hm_staff_scope(tenant_id,user_id,template_code,updated_by) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE template_code=VALUES(template_code),updated_by=VALUES(updated_by),updated_at=CURRENT_TIMESTAMP", repo.tenant(),g.userId(),t.code(),SecurityFrameworkUtils.getLoginUserId());
        repo.jdbc().update("DELETE FROM hm_staff_store WHERE tenant_id=? AND user_id=?",repo.tenant(),g.userId());
        for(long id:g.storeIds()) repo.jdbc().update("INSERT INTO hm_staff_store(tenant_id,user_id,store_id) VALUES(?,?,?)",repo.tenant(),g.userId(),id);
        repo.jdbc().update("INSERT INTO hm_staff_access_log(tenant_id,user_id,template_code,store_ids,operator_id) VALUES(?,?,?,?,?)",repo.tenant(),g.userId(),t.code(),new TreeSet<>(g.storeIds()).toString(),SecurityFrameworkUtils.getLoginUserId());
    }
}
