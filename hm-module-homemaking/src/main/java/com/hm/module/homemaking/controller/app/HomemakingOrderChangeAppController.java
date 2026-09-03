package com.hm.module.homemaking.controller.app;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.service.OrderChangeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/homemaking/orders")
public class HomemakingOrderChangeAppController {
    private final OrderChangeService changes;
    public HomemakingOrderChangeAppController(OrderChangeService changes){this.changes=changes;}
    @PostMapping("/{id}/address-change/preview")
    public CommonResult<?> preview(@PathVariable long id,@Valid @RequestBody OrderChangeService.CustomerRequest r){return success(changes.preview(id,changes.customerRequest(id,r),false,false));}
    @PostMapping("/{id}/address-change")
    public CommonResult<?> create(@PathVariable long id,@Valid @RequestBody OrderChangeService.CustomerRequest r){return success(changes.create(id,changes.customerRequest(id,r),false,false));}
    @PostMapping("/{id}/changes/{changeId}/cancel")
    public CommonResult<?> cancel(@PathVariable long id,@PathVariable long changeId,@Valid @RequestBody OrderChangeService.Cancel r){changes.cancel(id,changeId,r.reason(),false,false);return success(true);}
}
