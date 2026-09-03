package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.service.WorkerTimeOffService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/homemaking/workers/{workerId}/time-off")
@PreAuthorize("denyAll()")
public class HomemakingTimeOffController {
    private final WorkerTimeOffService service;
    public HomemakingTimeOffController(WorkerTimeOffService service){this.service=service;}
    @GetMapping @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:read')")
    public CommonResult<?> list(@PathVariable long workerId,@RequestParam LocalDate from,@RequestParam LocalDate to){return success(service.list(workerId,from,to));}
    @PostMapping("/{id}/review") @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:review')")
    public CommonResult<?> review(@PathVariable long workerId,@PathVariable long id,@Valid @RequestBody WorkerTimeOffService.Decision decision){service.review(workerId,id,decision);return success(true);}
    @PostMapping("/{id}/cancel") @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:review')")
    public CommonResult<?> cancel(@PathVariable long workerId,@PathVariable long id,@Valid @RequestBody WorkerTimeOffService.Cancel cancel){service.cancel(workerId,id,cancel,false);return success(true);}
}
