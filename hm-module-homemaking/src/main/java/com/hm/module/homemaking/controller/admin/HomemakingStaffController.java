package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.security.HomemakingAdminAccess;
import com.hm.module.homemaking.service.StaffAccessService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/homemaking/access")
@PreAuthorize("denyAll()")
public class HomemakingStaffController {
    private final HomemakingAdminAccess access; private final StaffAccessService staff;
    public HomemakingStaffController(HomemakingAdminAccess access, StaffAccessService staff) { this.access=access; this.staff=staff; }
    @GetMapping("/me") @PreAuthorize("isAuthenticated()")
    public CommonResult<?> me() { return success(access.current()); }
    @GetMapping("/templates") @PreAuthorize("@hmAdmin.allowed('homemaking:staff:read')")
    public CommonResult<?> templates() { return success(staff.templates()); }
    @GetMapping("/users") @PreAuthorize("@hmAdmin.allowed('homemaking:staff:read')")
    public CommonResult<?> users() { return success(staff.users()); }
    @GetMapping("/users/{id}") @PreAuthorize("@hmAdmin.allowed('homemaking:staff:read')")
    public CommonResult<?> binding(@PathVariable long id) { return success(staff.binding(id)); }
    @PutMapping("/users") @PreAuthorize("@hmAdmin.allowed('homemaking:staff:write')")
    public CommonResult<?> grant(@Valid @RequestBody StaffAccessService.Grant grant) { staff.grant(grant); return success(true); }
}
