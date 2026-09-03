package com.hm.module.system.service.permission;

import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Global, explicit authorization. Account tenancy is an identity partition, never platform authority. */
@Service("platformAccess")
public class PlatformAccessService {
    private final JdbcTemplate jdbc;
    public PlatformAccessService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Grant(@Min(1) long userId, @Min(1) long accountTenantId, boolean enabled,
                        @Min(0) long version, @NotBlank @Size(max=500) String reason) {}
    public boolean isOperator(Long user, Long accountTenant) {
        if (user == null || accountTenant == null) return false;
        return jdbc.queryForObject("SELECT COUNT(*) FROM system_platform_operator p JOIN system_users u ON u.id=p.user_id AND u.tenant_id=p.account_tenant_id WHERE p.user_id=? AND p.account_tenant_id=? AND p.enabled=TRUE AND u.status=0 AND u.deleted=FALSE", Long.class, user, accountTenant) == 1;
    }
    public boolean current() {
        var u = SecurityFrameworkUtils.getLoginUser();
        return u != null && Objects.equals(u.getUserType(), 2) && isOperator(u.getId(), u.getTenantId());
    }
    public void require() { denyUnless(current()); }
    public boolean canVisit(Long user, Long accountTenant, Long target) {
        return target != null && target > 0 && isOperator(user, accountTenant)
            && jdbc.queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=? AND status=0 AND deleted=FALSE AND expire_time>CURRENT_TIMESTAMP", Long.class, target) == 1;
    }
    private boolean protectedUser(long user) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM system_platform_operator WHERE user_id=? AND enabled=TRUE", Long.class, user) > 0;
    }
    /** Tenant administrators must not acquire a platform identity by changing its credentials. */
    public void guardUserMutation(long user) {
        lockAccount(user);
        denyUnless(!protectedUser(user) || current());
    }
    public void guardUserRemoval(long user) {
        lockAccount(user);
        if (protectedUser(user)) throw new AccessDeniedException("请先在平台身份管理撤销平台授权，再停用或删除账号");
    }
    private void lockAccount(long user) { jdbc.queryForList("SELECT id FROM system_users WHERE id=? FOR UPDATE", user); }
    public Map<String,Object> me() {
        var user = Objects.requireNonNull(SecurityFrameworkUtils.getLoginUser());
        return Map.of("platform", current(), "accountTenantId", user.getTenantId(),
            "businessTenantId", TenantContextHolder.getRequiredTenantId(), "userId", user.getId(),
            "businessTenantName", jdbc.queryForObject("SELECT name FROM system_tenant WHERE id=? AND deleted=FALSE",String.class,TenantContextHolder.getRequiredTenantId()));
    }
    public List<Map<String,Object>> operators() {
        require();
        return jdbc.queryForList("SELECT p.*,u.username,u.nickname,u.status AS user_status,u.deleted AS user_deleted,t.name AS tenant_name FROM system_platform_operator p LEFT JOIN system_users u ON u.id=p.user_id AND u.tenant_id=p.account_tenant_id LEFT JOIN system_tenant t ON t.id=p.account_tenant_id ORDER BY p.enabled DESC,p.user_id");
    }
    public List<Map<String,Object>> candidates(long tenant, String query) {
        require();
        if (query == null || query.trim().length() < 2 || query.length() > 80) return List.of();
        return jdbc.queryForList("SELECT id,username,nickname FROM system_users WHERE tenant_id=? AND status=0 AND deleted=FALSE AND (LOCATE(?,username)>0 OR LOCATE(?,nickname)>0) ORDER BY id LIMIT 30", tenant, query.trim(), query.trim());
    }
    @Transactional
    public void grant(Grant g, String ip) {
        // A singleton row serializes grants/revocations, including two administrators revoking each other.
        jdbc.queryForObject("SELECT id FROM system_platform_access_lock WHERE id=0 FOR UPDATE", Long.class);
        require();
        long actor = SecurityFrameworkUtils.getLoginUserId();
        denyUnless(g.userId() != actor);
        lockAccount(g.userId());
        var previous = jdbc.queryForList("SELECT * FROM system_platform_operator WHERE user_id=? FOR UPDATE", g.userId());
        long version = previous.isEmpty() ? 0 : ((Number) previous.get(0).get("version")).longValue();
        if (version != g.version()) throw new IllegalArgumentException("平台授权已变化，请刷新后重试");
        if (!previous.isEmpty()) denyUnless(((Number)previous.get(0).get("account_tenant_id")).longValue() == g.accountTenantId());
        if (g.enabled()) denyUnless(jdbc.queryForObject("SELECT COUNT(*) FROM system_users u JOIN system_tenant t ON t.id=u.tenant_id WHERE u.id=? AND u.tenant_id=? AND u.deleted=FALSE AND u.status=0 AND t.deleted=FALSE AND t.status=0 AND t.expire_time>CURRENT_TIMESTAMP", Long.class, g.userId(), g.accountTenantId()) == 1);
        else denyUnless(!previous.isEmpty() && Boolean.TRUE.equals(previous.get(0).get("enabled")));
        boolean before = !previous.isEmpty() && Boolean.TRUE.equals(previous.get(0).get("enabled"));
        if (before == g.enabled()) throw new IllegalArgumentException("授权状态未变化，请刷新");
        if (previous.isEmpty()) jdbc.update("INSERT INTO system_platform_operator(user_id,account_tenant_id,enabled,version,updated_by,reason) VALUES(?,?,?,1,?,?)", g.userId(), g.accountTenantId(), g.enabled(), actor, g.reason());
        else jdbc.update("UPDATE system_platform_operator SET enabled=?,version=version+1,updated_by=?,reason=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=?", g.enabled(), actor, g.reason(), g.userId());
        audit(g.enabled() ? "GRANT" : "REVOKE", g.accountTenantId(), g.userId(), Boolean.toString(before), Boolean.toString(g.enabled()), g.reason(), ip, "PUT", "/system/platform-access/operators", 200);
    }
    public List<Map<String,Object>> logs(int page, int size) {
        require(); size=Math.min(Math.max(size,1),100); page=Math.min(Math.max(page,1),100000);
        return jdbc.queryForList("SELECT * FROM system_platform_access_log ORDER BY id DESC LIMIT ? OFFSET ?", size, (page-1)*size);
    }
    public void visitLog(long target, String method, String path, String ip, int status) {
        // The interceptor authenticated the actor before the request; record even if revoked during it.
        audit("VISIT", target, null, null, null, "跨租户访问", ip, method, path, status);
    }
    private void audit(String action, long target, Long object, String before, String after, String reason, String ip, String method, String path, int status) {
        var u=Objects.requireNonNull(SecurityFrameworkUtils.getLoginUser());
        jdbc.update("INSERT INTO system_platform_access_log(action,operator_id,account_tenant_id,target_tenant_id,target_user_id,before_value,after_value,reason,request_ip,request_method,request_path,response_status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
            action,u.getId(),u.getTenantId(),target,object,before,after,reason,limit(ip,64),limit(method,12),limit(path,500),status);
    }
    private static String limit(String s,int n) { return s==null?"":s.substring(0,Math.min(s.length(),n)); }
    private static void denyUnless(boolean allowed) { if(!allowed) throw new AccessDeniedException("需要有效的平台授权或目标账号不符合要求"); }
}
