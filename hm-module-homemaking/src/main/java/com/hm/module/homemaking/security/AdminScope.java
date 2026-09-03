package com.hm.module.homemaking.security;

import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

/** Request-local scope. Only the admin controller aspect installs it; customer APIs keep their own ownership checks. */
public record AdminScope(long tenant, boolean platform, String range, Set<Long> stores, Long worker) {
    private static final ThreadLocal<AdminScope> CURRENT = new ThreadLocal<>();
    public static AdminScope current() { return CURRENT.get(); }
    public static void set(AdminScope scope) { if (scope == null) CURRENT.remove(); else CURRENT.set(scope); }
    public static void denyUnless(boolean allowed) { if (!allowed) throw new AccessDeniedException("无此操作权限或数据范围"); }
    public static void tenant(long target) {
        var scope = current();
        if (scope != null) denyUnless(target == scope.tenant || scope.platform);
    }
    public static void platformOnly() { if (current() != null) denyUnless(current().platform); }
    public static void tenantWide() { if (current() != null) denyUnless(current().range.equals("TENANT")); }
    public static String sql(String table, String alias, long tenant) {
        var scope = current();
        if (scope == null) return "";
        denyUnless(scope.tenant == tenant);
        if (scope.range.equals("TENANT")) return "";
        String a = alias.isEmpty() ? table : alias;
        String ids = scope.stores.stream().filter(id -> id > 0).sorted().map(String::valueOf).collect(Collectors.joining(","));
        String store = ids.isEmpty() ? "1=0" : "%s IN (" + ids + ")";
        boolean self = scope.range.equals("SELF");
        String order = self ? "o.worker_id=" + Objects.requireNonNullElse(scope.worker, -1L) : store.formatted("o.store_id");
        String predicate = switch (table) {
            case "hm_store" -> self ? "1=0" : store.formatted(a + ".id");
            case "hm_worker" -> self ? a + ".id=" + Objects.requireNonNullElse(scope.worker, -1L) : store.formatted(a + ".store_id");
            case "hm_order", "hm_booking" -> self ? a + ".worker_id=" + Objects.requireNonNullElse(scope.worker, -1L) : store.formatted(a + ".store_id");
            case "hm_service", "hm_settlement" -> self ? "1=0" : store.formatted(a + ".store_id");
            case "hm_aftersale", "hm_review", "hm_notification_outbox", "hm_payment_entry" ->
                "EXISTS (SELECT 1 FROM hm_order o WHERE o.tenant_id=" + a + ".tenant_id AND o.id=" + a + ".order_id AND " + order + ")";
            case "hm_customer_tenant" -> "EXISTS (SELECT 1 FROM hm_order o WHERE o.tenant_id=" + a + ".tenant_id AND o.customer_id=" + a + ".customer_id AND " + order + ")";
            case "hm_settlement_statement" -> self ? "1=0" : "((" + a + ".beneficiary='STORE' AND " + store.formatted(a + ".beneficiary_id") + ") OR (" + a + ".beneficiary='WORKER' AND EXISTS (SELECT 1 FROM hm_worker w WHERE w.tenant_id=" + a + ".tenant_id AND w.id=" + a + ".beneficiary_id AND " + store.formatted("w.store_id") + ")))";
            default -> "1=0";
        };
        return " AND (" + predicate + ")";
    }
}
