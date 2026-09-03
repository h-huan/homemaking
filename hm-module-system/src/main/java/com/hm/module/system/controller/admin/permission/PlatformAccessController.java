package com.hm.module.system.controller.admin.permission;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.system.service.permission.PlatformAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/system/platform-access")
@PreAuthorize("@platformAccess.current()")
public class PlatformAccessController {
    private final PlatformAccessService access;
    public PlatformAccessController(PlatformAccessService access) { this.access=access; }
    @GetMapping("/me") @PreAuthorize("isAuthenticated()")
    public CommonResult<?> me() { return success(access.me()); }
    @GetMapping("/operators") public CommonResult<?> operators() { return success(access.operators()); }
    @GetMapping("/candidates") public CommonResult<?> candidates(@RequestParam long tenantId, @RequestParam String query) { return success(access.candidates(tenantId,query)); }
    @PutMapping("/operators") public CommonResult<?> grant(@Valid @RequestBody PlatformAccessService.Grant grant,HttpServletRequest request) { access.grant(grant,request.getRemoteAddr());return success(true); }
    @GetMapping("/logs") public CommonResult<?> logs(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="30") int size) { return success(access.logs(page,size)); }
}
