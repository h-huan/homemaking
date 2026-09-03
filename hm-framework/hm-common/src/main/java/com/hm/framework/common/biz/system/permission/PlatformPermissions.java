package com.hm.framework.common.biz.system.permission;

import java.util.Set;

/** Global administration cannot be acquired through a tenant role or an over-granted menu. */
public final class PlatformPermissions {
    private PlatformPermissions() {}
    private static final Set<String> PREFIXES=Set.of("system:tenant:","system:tenant-package:",
        "system:platform-access:","homemaking:platform:","system:menu:","system:dict-",
        "system:sms-","system:mail-","system:notify-template:","system:oauth2-client:",
        "infra:file-config:","infra:data-source-config:");
    public static boolean reserved(String permission) {
        return permission!=null && PREFIXES.stream().anyMatch(permission::startsWith);
    }
}
