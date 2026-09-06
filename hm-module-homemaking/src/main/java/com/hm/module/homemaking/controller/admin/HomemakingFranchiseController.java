package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.service.FranchiseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.hm.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/homemaking/franchises")
@Validated
@PreAuthorize("denyAll()")
public class HomemakingFranchiseController {
    public record Version(@Min(0) long version) {}
    public record Reveal(@NotBlank @Size(max=500) String reason) {}
    private final FranchiseService service;
    public HomemakingFranchiseController(FranchiseService service){this.service=service;}

    @GetMapping("/tenants") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:read')")
    public CommonResult<?> tenants(){return success(service.tenantCandidates());}
    @GetMapping @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:read')")
    public CommonResult<?> list(){return success(service.list());}
    @GetMapping("/{id}") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:read')")
    public CommonResult<?> detail(@PathVariable long id){return success(service.detail(id));}
    @PostMapping @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:write')")
    public CommonResult<Long> create(@Valid @RequestBody FranchiseService.Profile request){return success(service.create(request));}
    @PutMapping("/{id}") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:write')")
    public CommonResult<?> update(@PathVariable long id,@Valid @RequestBody FranchiseService.Profile request){service.update(id,request);return success(true);}
    @PostMapping("/{id}/sensitive") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:settlement')")
    public CommonResult<?> reveal(@PathVariable long id,@Valid @RequestBody Reveal request){return success(service.reveal(id,request.reason()));}

    @PostMapping("/{id}/contracts") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:contract')")
    public CommonResult<Long> createContract(@PathVariable long id,@Valid @RequestBody FranchiseService.Contract request){return success(service.createContract(id,request));}
    @PutMapping("/{id}/contracts/{contractId}") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:contract')")
    public CommonResult<?> updateContract(@PathVariable long id,@PathVariable long contractId,@Valid @RequestBody FranchiseService.Contract request){service.updateContract(id,contractId,request);return success(true);}
    @PostMapping("/{id}/contracts/{contractId}/activate") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:contract')")
    public CommonResult<?> activate(@PathVariable long id,@PathVariable long contractId,@Valid @RequestBody Version request){service.activate(id,contractId,request.version());return success(true);}
    @PostMapping("/{id}/contracts/{contractId}/renew") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:contract')")
    public CommonResult<Long> renew(@PathVariable long id,@PathVariable long contractId,@Valid @RequestBody FranchiseService.Renewal request){return success(service.renew(id,contractId,request));}
    @PostMapping("/{id}/contracts/{contractId}/terminate") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:contract')")
    public CommonResult<?> terminateContract(@PathVariable long id,@PathVariable long contractId,@Valid @RequestBody FranchiseService.Reason request){service.terminateContract(id,contractId,request);return success(true);}
    @PostMapping("/{id}/contracts/{contractId}/deposits") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:settlement')")
    public CommonResult<Long> deposit(@PathVariable long id,@PathVariable long contractId,@Valid @RequestBody FranchiseService.Deposit request){return success(service.recordDeposit(id,contractId,request));}
    @PostMapping("/{id}/terminate") @PreAuthorize("@hmAdmin.allowed('homemaking:franchise:write')")
    public CommonResult<?> terminate(@PathVariable long id,@Valid @RequestBody FranchiseService.Reason request){service.terminateFranchise(id,request);return success(true);}
}
