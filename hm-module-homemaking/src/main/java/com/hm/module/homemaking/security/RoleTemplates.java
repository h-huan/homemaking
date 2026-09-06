package com.hm.module.homemaking.security;

import java.util.*;

/** Templates are an upper bound as well as initial system-menu grants. */
public final class RoleTemplates {
    private RoleTemplates() {}
    public record Template(String code, String name, String range, Set<String> permissions) {}
    private static Set<String> permissions(String... groups) {
        var result = new LinkedHashSet<String>();
        for (String group : groups) {
            String[] parts = group.split(":");
            for (String action : parts[1].split(",")) result.add("homemaking:" + parts[0] + ":" + action);
        }
        return Collections.unmodifiableSet(result);
    }
    public static final Set<String> ALL = permissions("stores:read,write", "workers:read,write,bind", "services:read,write",
            "orders:read,dispatch,reschedule,cancel,fulfill,change,price", "customers:read", "aftersales:read,process,reject,refund",
            "reviews:read,reply,moderate", "schedule:read,write,templates,review", "finance:read,statement,approve,payout,reconcile",
            "brand:read,write", "notification:read,write", "portal:read,write,publish", "staff:read,write",
            "quota:read", "platform:manage", "franchise:read,write,contract,settlement",
            "worker:read,fulfill,leave,income", "payment:read,receive,reverse,configure");
    private static Set<String> owner() {
        var p = new LinkedHashSet<>(ALL);
        p.remove("homemaking:platform:manage"); p.removeIf(permission -> permission.startsWith("homemaking:worker:"));
        p.removeIf(permission -> permission.startsWith("homemaking:franchise:"));
        return Collections.unmodifiableSet(p);
    }
    public static final List<Template> ALL_TEMPLATES = List.of(
        new Template("PLATFORM", "平台管理员", "TENANT", ALL),
        new Template("OWNER", "租户老板", "TENANT", owner()),
        new Template("MANAGER", "店长", "STORES", permissions("stores:read,write", "workers:read,write,bind", "services:read,write", "orders:read,dispatch,reschedule,cancel,fulfill,change,price", "customers:read", "aftersales:read,process,reject", "reviews:read,reply,moderate", "schedule:read,write,review", "payment:read,receive")),
        new Template("DISPATCHER", "调度", "STORES", permissions("stores:read", "workers:read", "services:read", "orders:read,dispatch,reschedule", "schedule:read,write")),
        new Template("SUPPORT", "客服", "STORES", permissions("stores:read", "workers:read", "services:read", "orders:read,reschedule,cancel,change", "customers:read", "aftersales:read,process,reject", "reviews:read,reply", "schedule:read")),
        new Template("FINANCE", "财务", "STORES", permissions("stores:read", "workers:read", "orders:read", "aftersales:read,refund", "finance:read,statement,approve,payout,reconcile", "payment:read,receive,reverse")),
        new Template("WORKER", "服务人员", "SELF", permissions("worker:read,fulfill,leave,income"))
    );
    public static Template get(String code) { return ALL_TEMPLATES.stream().filter(t -> t.code.equals(code)).findFirst().orElseThrow(() -> new IllegalArgumentException("未知角色模板")); }
}
