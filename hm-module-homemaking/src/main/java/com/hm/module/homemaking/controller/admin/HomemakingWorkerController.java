package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.service.WorkerService;
import com.hm.module.homemaking.service.WorkerWorkbenchService;
import com.hm.module.homemaking.service.WorkerTimeOffService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/homemaking/worker")
@PreAuthorize("@hmAdmin.allowed('homemaking:worker:read')")
public class HomemakingWorkerController {
    private final WorkerService workers;
    @org.springframework.beans.factory.annotation.Autowired private WorkerWorkbenchService workbench;
    public HomemakingWorkerController(WorkerService workers){this.workers=workers;}
    @GetMapping("/calendar") public CommonResult<?> calendar(@RequestParam LocalDate from,@RequestParam LocalDate to){return success(workbench.calendar(from,to));}
    @GetMapping("/income") @PreAuthorize("@hmAdmin.allowed('homemaking:worker:income')")
    public CommonResult<?> income(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(defaultValue="ENTRIES") String view,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(workbench.income(from,to,view,page,size));}
    @PostMapping("/time-off") @PreAuthorize("@hmAdmin.allowed('homemaking:worker:leave')")
    public CommonResult<?> request(@Valid @RequestBody WorkerTimeOffService.Request request){return success(workbench.request(request));}
    @PostMapping("/time-off/{id}/cancel") @PreAuthorize("@hmAdmin.allowed('homemaking:worker:leave')")
    public CommonResult<?> cancel(@PathVariable long id,@Valid @RequestBody WorkerTimeOffService.Cancel cancel){workbench.cancel(id,cancel);return success(true);}
    @GetMapping("/tasks") public CommonResult<?> tasks(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return success(workers.tasks(from,to));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:worker:fulfill')")
    @PostMapping("/orders/{id}/action") public CommonResult<?> action(@PathVariable long id,@Valid @RequestBody WorkerService.Action action){workers.action(id,action);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:worker:fulfill')")
    @PostMapping(value="/orders/{id}/evidence",consumes="multipart/form-data") public CommonResult<?> evidence(@PathVariable long id,@RequestParam String phase,@RequestParam(defaultValue="") String note,@RequestParam org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {return success(workers.uploadEvidence(id,phase,note,file.getBytes()));}
    @GetMapping("/orders/{id}/evidence") public CommonResult<?> evidence(@PathVariable long id){return success(workers.evidence(id,false));}
    @GetMapping("/orders/{id}/evidence/{evidenceId}/content") public org.springframework.http.ResponseEntity<byte[]> content(@PathVariable long id,@PathVariable long evidenceId){return workers.content(id,evidenceId,false,false);}
}
