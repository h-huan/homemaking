package com.hm.module.homemaking.controller.app;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.service.*;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.module.homemaking.dal.HmRepository.check;

@RestController @RequestMapping("/homemaking") @Validated
public class HomemakingAppController {
    private final CatalogService catalog;private final OrderService orders;private final PaymentService payments;private final CustomerAccess access;private final IdentityService identity;private final HmRepository repo;
    public HomemakingAppController(CatalogService catalog,OrderService orders,PaymentService payments,CustomerAccess access,IdentityService identity,HmRepository repo){this.catalog=catalog;this.orders=orders;this.payments=payments;this.access=access;this.identity=identity;this.repo=repo;}
    @PermitAll @GetMapping("/catalog/{kind}") public CommonResult<?> catalog(@PathVariable String kind,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(catalog.list(kind,page,size,true));}
    @GetMapping("/me") public CommonResult<?> me(){return success(repo.jdbc().queryForMap("SELECT id,nickname,avatar,mobile FROM hm_customer WHERE id=?",access.current()));}
    public record Login(@NotBlank @Size(max=100) String appId,@NotBlank @Size(max=200) String code){}
    @com.hm.framework.ratelimiter.core.annotation.RateLimiter(time=60,count=15,keyResolver=com.hm.module.homemaking.config.HmLoginRateLimitKeyResolver.class)
    @com.hm.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false)
    @PermitAll @PostMapping("/wechat/mini-login") public CommonResult<?> login(@Valid @RequestBody Login request){return success(identity.login(request.appId(),"MINI",request.code()));}
    @com.hm.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false)
    @PostMapping("/wechat/phone") public CommonResult<?> phone(@Valid @RequestBody Login request){identity.bindPhone(request.appId(),request.code());return success(true);}
    @GetMapping("/addresses") public CommonResult<?> addresses(){return success(orders.addresses());}
    @PostMapping("/addresses") public CommonResult<?> address(@Valid @RequestBody OrderService.Address address){return success(orders.saveAddress(address));}
    @PostMapping("/bookings") public CommonResult<?> book(@Valid @RequestBody OrderService.Book request){return success(orders.book(request));}
    @GetMapping("/orders") public CommonResult<?> orders(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(orders.list(page,size,false));}
    @GetMapping("/orders/{id}") public CommonResult<?> order(@PathVariable long id){return success(orders.detail(id,false));}
    @PostMapping("/orders/{id}/cancel") public CommonResult<?> cancel(@PathVariable long id){orders.cancel(id,false);return success(true);}
    @PostMapping("/orders/{id}/pay") public CommonResult<?> pay(@PathVariable long id,HttpServletRequest request){return success(payments.create(id,request.getRemoteAddr()));}
    public record MiniPay(@NotBlank @Size(max=100) String appId){}
    @PostMapping("/orders/{id}/mini-pay") public CommonResult<?> miniPay(@PathVariable long id,@Valid @RequestBody MiniPay payment,HttpServletRequest request){return success(payments.miniPay(id,payment.appId(),request.getRemoteAddr()));}
    @PostMapping("/orders/{id}/sync-pay") public CommonResult<?> sync(@PathVariable long id){payments.syncOwned(id);return success(true);}
    @PostMapping("/reviews") public CommonResult<?> review(@Valid @RequestBody OrderService.Review review){return success(orders.review(review));}
    @PostMapping("/aftersales") public CommonResult<?> aftersale(@Valid @RequestBody OrderService.Aftersale aftersale){return success(orders.aftersale(aftersale));}
    public record Preference(@Pattern(regexp="[A-Z_]{1,60}") String event,boolean enabled,boolean allowSms){}
    @GetMapping("/notification-preferences") public CommonResult<?> preferences(){return success(repo.jdbc().queryForList("SELECT event_type,enabled,allow_sms FROM hm_notification_preference WHERE tenant_id=? AND customer_id=?",repo.tenant(),access.current()));}
    @PutMapping("/notification-preference") public CommonResult<?> preference(@Valid @RequestBody Preference preference){repo.jdbc().update("INSERT INTO hm_notification_preference(tenant_id,customer_id,event_type,enabled,allow_sms) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),allow_sms=VALUES(allow_sms)",repo.tenant(),access.current(),preference.event(),preference.enabled(),preference.allowSms());return success(true);}
    public record Consent(@NotBlank String appId,@NotBlank String templateId,boolean accepted){}
    @GetMapping("/wechat/subscription-templates") public CommonResult<?> templates(@RequestParam String appId){long customer=access.current();
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_wechat_app a JOIN hm_wechat_identity i ON i.app_id=a.app_id WHERE a.tenant_id=? AND a.app_id=? AND a.kind='MINI' AND a.enabled=TRUE AND i.customer_id=?",Long.class,repo.tenant(),appId,customer)==1,"微信身份未关联");
        return success(repo.jdbc().queryForList("SELECT DISTINCT template_id FROM hm_notification_template WHERE tenant_id=? AND channel='MINI' AND enabled=TRUE AND event_type IN ('SERVICE_REMINDER','WORKER_CHANGED','REFUND_RESULT') ORDER BY template_id LIMIT 3",String.class,repo.tenant()));}
    @PostMapping("/wechat/subscription") public CommonResult<?> consent(@Valid @RequestBody Consent consent){long customer=access.current();check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_wechat_app a JOIN hm_wechat_identity i ON i.app_id=a.app_id WHERE a.tenant_id=? AND a.app_id=? AND a.kind='MINI' AND i.customer_id=?",Long.class,repo.tenant(),consent.appId(),customer)==1,"微信身份未关联");
        // One reported consent permits at most one attempt; the provider remains the authority on subscription acceptance.
        repo.jdbc().update("INSERT INTO hm_notification_consent(tenant_id,customer_id,app_id,template_id,remaining) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE remaining=VALUES(remaining)",repo.tenant(),customer,consent.appId(),consent.templateId(),consent.accepted()?1:0);return success(true);}
}
