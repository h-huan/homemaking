package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.service.WorkerService;
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
    public HomemakingWorkerController(WorkerService workers){this.workers=workers;}
    @GetMapping("/tasks") public CommonResult<?> tasks(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return success(workers.tasks(from,to));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:worker:fulfill')")
    @PostMapping("/orders/{id}/action") public CommonResult<?> action(@PathVariable long id,@Valid @RequestBody WorkerService.Action action){workers.action(id,action);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:worker:fulfill')")
    @PostMapping(value="/orders/{id}/evidence",consumes="multipart/form-data") public CommonResult<?> evidence(@PathVariable long id,@RequestParam String phase,@RequestParam(defaultValue="") String note,@RequestParam org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {return success(workers.uploadEvidence(id,phase,note,file.getBytes()));}
    @GetMapping("/orders/{id}/evidence") public CommonResult<?> evidence(@PathVariable long id){return success(workers.evidence(id,false));}
    @GetMapping("/orders/{id}/evidence/{evidenceId}/content") public org.springframework.http.ResponseEntity<byte[]> content(@PathVariable long id,@PathVariable long evidenceId){return workers.content(id,evidenceId,false,false);}
}
