package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.service.*;
import com.hm.module.homemaking.security.AdminScope;
import com.hm.framework.tenant.core.util.TenantUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import static com.hm.framework.common.pojo.CommonResult.success;

@RestController @RequestMapping("/homemaking") @PreAuthorize("denyAll()")
public class HomemakingPaymentController {
    private final PaymentPolicyService policy;private final PaymentLedgerService ledger;
    public HomemakingPaymentController(PaymentPolicyService policy,PaymentLedgerService ledger){this.policy=policy;this.ledger=ledger;}
    @GetMapping("/payment-settings") @PreAuthorize("@hmAdmin.allowed('homemaking:payment:configure')")
    public CommonResult<?> settings(){return success(policy.current());}
    @PutMapping("/payment-settings") @PreAuthorize("@hmAdmin.allowed('homemaking:payment:configure')")
    public CommonResult<?> settings(@Valid @RequestBody PaymentPolicyService.Settings settings){policy.save(settings);return success(true);}
    @GetMapping("/orders/{id}/payments") @PreAuthorize("@hmAdmin.allowed('homemaking:payment:read')")
    public CommonResult<?> entries(@PathVariable long id){return success(ledger.orderEntries(id));}
    @PostMapping("/orders/{id}/offline-receipt") @PreAuthorize("@hmAdmin.allowed('homemaking:payment:receive')")
    public CommonResult<?> receive(@PathVariable long id,@Valid @RequestBody PaymentLedgerService.Receipt receipt){return success(ledger.receive(id,receipt));}
    @PostMapping("/orders/{id}/reverse-receipt") @PreAuthorize("@hmAdmin.allowed('homemaking:payment:reverse')")
    public CommonResult<?> reverse(@PathVariable long id,@Valid @RequestBody PaymentLedgerService.Reversal reversal){return success(ledger.reverse(id,reversal));}
    @PostMapping("/aftersales/{id}/offline-refund") @PreAuthorize("@hmAdmin.allowed('homemaking:aftersales:refund')")
    public CommonResult<?> refund(@PathVariable long id,@Valid @RequestBody PaymentLedgerService.Receipt receipt){return success(ledger.refund(id,receipt));}
    @GetMapping("/payment-ledger") @PreAuthorize("@hmAdmin.allowed('homemaking:finance:read') and (#tenantId == null or @hmAdmin.tenant(#tenantId))")
    public CommonResult<?> report(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) Long tenantId){
        var scope=AdminScope.current();long target=tenantId==null?scope.tenant():tenantId;AdminScope.tenant(target);
        return TenantUtils.execute(target,()->{
            try{AdminScope.set(new AdminScope(target,scope.platform(),scope.range(),scope.stores(),scope.worker()));return success(ledger.report(from,to,page,size));}
            finally{AdminScope.set(scope);}
        });
    }
}
