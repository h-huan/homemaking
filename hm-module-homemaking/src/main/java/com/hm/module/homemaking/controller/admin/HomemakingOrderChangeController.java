package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.security.HomemakingAdminAccess;
import com.hm.module.homemaking.service.OrderChangeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/homemaking/orders") @PreAuthorize("denyAll()")
public class HomemakingOrderChangeController {
    private final OrderChangeService changes;private final HomemakingAdminAccess access;
    public HomemakingOrderChangeController(OrderChangeService changes,HomemakingAdminAccess access){this.changes=changes;this.access=access;}
    @PostMapping("/{id}/changes/preview") @PreAuthorize("@hmAdmin.allowed('homemaking:orders:change')")
    public CommonResult<?> preview(@PathVariable long id,@Valid @RequestBody OrderChangeService.Request r){return success(changes.preview(id,r,true,access.allowed("homemaking:orders:price")));}
    @PostMapping("/{id}/changes") @PreAuthorize("@hmAdmin.allowed('homemaking:orders:change')")
    public CommonResult<?> create(@PathVariable long id,@Valid @RequestBody OrderChangeService.Request r){return success(changes.create(id,r,true,access.allowed("homemaking:orders:price")));}
    @PostMapping("/{id}/changes/{changeId}/cancel") @PreAuthorize("@hmAdmin.allowed('homemaking:orders:change')")
    public CommonResult<?> cancel(@PathVariable long id,@PathVariable long changeId,@Valid @RequestBody OrderChangeService.Cancel r){changes.cancel(id,changeId,r.reason(),true,access.allowed("homemaking:orders:price"));return success(true);}
}
