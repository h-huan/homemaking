package com.hm.module.homemaking.controller.admin;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.module.homemaking.dal.HmRepository.*;

@RestController @RequestMapping("/homemaking") @Validated
@PreAuthorize("@ss.hasPermission('homemaking:manage')")
public class HomemakingAdminController {
    private final CatalogService catalog;private final OrderService orders;private final PaymentService payments;private final BrandingService branding;private final NotificationService notifications;private final HmRepository repo;
    public HomemakingAdminController(CatalogService catalog,OrderService orders,PaymentService payments,BrandingService branding,NotificationService notifications,HmRepository repo){this.catalog=catalog;this.orders=orders;this.payments=payments;this.branding=branding;this.notifications=notifications;this.repo=repo;}
    @GetMapping("/catalog/{kind}") public CommonResult<?> catalog(@PathVariable String kind,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(catalog.list(kind,page,size,false));}
    @PostMapping("/catalog/{kind}") public CommonResult<Long> save(@PathVariable String kind,@Valid @RequestBody CatalogService.Save request){return success(catalog.save(kind,request));}
    @GetMapping("/orders") public CommonResult<?> orders(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(orders.list(page,size,true));}
    @GetMapping("/orders/{id}") public CommonResult<?> order(@PathVariable long id){return success(orders.detail(id,true));}
    public record Assign(@NotNull Long workerId){}
    @PostMapping("/orders/{id}/assign") public CommonResult<?> assign(@PathVariable long id,@Valid @RequestBody Assign a){orders.assign(id,a.workerId());return success(true);}
    @PostMapping("/orders/{id}/start") public CommonResult<?> start(@PathVariable long id){orders.start(id);return success(true);}
    @PostMapping("/orders/{id}/complete") public CommonResult<?> complete(@PathVariable long id){orders.complete(id);return success(true);}
    @PostMapping("/orders/{id}/cancel") public CommonResult<?> cancel(@PathVariable long id){orders.cancel(id,true);return success(true);}
    @GetMapping("/aftersales") public CommonResult<?> aftersales(){return success(repo.jdbc().queryForList("SELECT * FROM hm_aftersale WHERE tenant_id=? ORDER BY id DESC LIMIT 100",repo.tenant()));}
    @PostMapping("/aftersales/{id}/approve") public CommonResult<?> approve(@PathVariable long id,HttpServletRequest request){payments.approveRefund(id,request.getRemoteAddr());return success(true);}
    @PostMapping("/aftersales/{id}/sync") public CommonResult<?> sync(@PathVariable long id){payments.syncRefund(id);return success(true);}
    @GetMapping("/settlements") public CommonResult<?> settlements(){return success(repo.jdbc().queryForList("SELECT * FROM hm_settlement WHERE tenant_id=? ORDER BY id DESC LIMIT 100",repo.tenant()));}
    @GetMapping("/customers") public CommonResult<?> customers(){return success(repo.jdbc().queryForList("SELECT c.id,c.nickname,c.avatar,r.status,r.source,r.created_at FROM hm_customer_tenant r JOIN hm_customer c ON c.id=r.customer_id WHERE r.tenant_id=? ORDER BY r.created_at DESC LIMIT 100",repo.tenant()));}
    @GetMapping("/brand") public CommonResult<?> brand(){return success(branding.current());}
    @PutMapping("/brand") public CommonResult<?> brand(@Valid @RequestBody BrandingService.Brand brand){branding.save(brand);return success(true);}
    public record Domain(@NotBlank @Size(max=253) String domain){}
    @PostMapping("/domains") public CommonResult<?> domain(@Valid @RequestBody Domain domain){return success(branding.requestDomain(domain.domain()));}
    @PostMapping("/domains/verify") public CommonResult<?> verify(@Valid @RequestBody Domain domain){branding.verifyDomain(domain.domain());return success(true);}
    @GetMapping("/notifications") public CommonResult<?> notifications(){return success(repo.jdbc().queryForList("SELECT id,customer_id,order_id,event_type,level,status,attempts,next_attempt_at,sent_channel,last_error,created_at FROM hm_notification_outbox WHERE tenant_id=? ORDER BY id DESC LIMIT 100",repo.tenant()));}
    @GetMapping("/notification-policy") public CommonResult<?> policy(@RequestParam(defaultValue="*") String event){return success(Map.of("platform",notifications.policy(0,event),"tenant",notifications.policy(repo.tenant(),event)));}
    @PutMapping("/notification-policy") public CommonResult<?> policy(@RequestParam String event,@RequestParam(defaultValue="false") boolean platform,@RequestBody NotificationPolicy policy){notifications.savePolicy(event,policy,platform);return success(true);}
    public record Template(@Pattern(regexp="[A-Z_]{1,60}") String event,@Pattern(regexp="MP|MINI|SMS") String channel,@NotBlank @Size(max=200) String templateId,@NotNull Map<String,String> fieldMapping,boolean enabled){}
    @PutMapping("/notification-template") public CommonResult<?> template(@Valid @RequestBody Template template) throws Exception {
        String mapping=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(template.fieldMapping());check(mapping.length()<=4000,"模板映射过长");
        repo.jdbc().update("INSERT INTO hm_notification_template(tenant_id,event_type,channel,template_id,field_mapping,enabled) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE template_id=VALUES(template_id),field_mapping=VALUES(field_mapping),enabled=VALUES(enabled)",repo.tenant(),template.event(),template.channel(),template.templateId(),mapping,template.enabled());return success(true);
    }
    public record WechatApp(@Min(1) long tenantId,@Pattern(regexp="wx[A-Za-z0-9]{10,32}") String appId,@Pattern(regexp="MINI|MP") String kind,@NotBlank @Size(max=100) String platformId,@Pattern(regexp="HM_WECHAT_(MINI|MP)(_[A-Z0-9]+)*_SECRET") String secretEnv,boolean enabled){}
    @PostMapping("/wechat-apps") public CommonResult<?> app(@Valid @RequestBody WechatApp app){check(repo.tenant()==1,"仅总部可分配微信应用及开放平台归属");check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=? AND deleted=FALSE",Long.class,app.tenantId())==1,"租户不存在");
        repo.jdbc().update("INSERT INTO hm_wechat_app(tenant_id,app_id,kind,platform_id,secret_env,enabled) VALUES(?,?,?,?,?,?)",app.tenantId(),app.appId(),app.kind(),app.platformId(),app.secretEnv(),app.enabled());return success(true);}
}
