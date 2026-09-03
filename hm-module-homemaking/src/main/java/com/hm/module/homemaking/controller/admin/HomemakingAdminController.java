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
import java.time.LocalDate;
import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.module.homemaking.dal.HmRepository.*;

@RestController @RequestMapping("/homemaking") @Validated
@PreAuthorize("denyAll()")
public class HomemakingAdminController {
    private final CatalogService catalog;private final OrderService orders;private final PaymentService payments;private final BrandingService branding;private final NotificationService notifications;private final HmRepository repo;private final ScheduleService schedules;private final WorkerService workers;private final QuotaService quotas;private final SettlementService settlementService;private final PortalService portal;private final ServiceSettingsService serviceSettings;
    public HomemakingAdminController(CatalogService catalog,OrderService orders,PaymentService payments,BrandingService branding,NotificationService notifications,HmRepository repo,ScheduleService schedules,WorkerService workers,QuotaService quotas,SettlementService settlementService,PortalService portal,ServiceSettingsService serviceSettings){this.catalog=catalog;this.orders=orders;this.payments=payments;this.branding=branding;this.notifications=notifications;this.repo=repo;this.schedules=schedules;this.workers=workers;this.quotas=quotas;this.settlementService=settlementService;this.portal=portal;this.serviceSettings=serviceSettings;}
    @PreAuthorize("@hmAdmin.catalog(#kind, 'read')")
    @GetMapping("/catalog/{kind}") public CommonResult<?> catalog(@PathVariable String kind,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(catalog.list(kind,page,size,false));}
    @PreAuthorize("@hmAdmin.catalog(#kind, 'write')")
    @PostMapping("/catalog/{kind}") public CommonResult<Long> save(@PathVariable String kind,@Valid @RequestBody CatalogService.Save request){return success(catalog.save(kind,request));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:services:read')")
    @GetMapping("/services/{id}/settings") public CommonResult<?> serviceSettings(@PathVariable long id){return success(serviceSettings.get(id));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:services:write')")
    @PutMapping("/services/{id}/settings") public CommonResult<?> serviceSettings(@PathVariable long id,@Valid @RequestBody ServiceSettingsService.Settings settings){serviceSettings.save(id,settings);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:read')")
    @GetMapping("/orders") public CommonResult<?> orders(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return success(orders.list(page,size,true));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:read')")
    @GetMapping("/orders/{id}") public CommonResult<?> order(@PathVariable long id){return success(orders.detail(id,true));}
    public record Assign(@NotNull Long workerId){}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:dispatch')")
    @PostMapping("/orders/{id}/assign") public CommonResult<?> assign(@PathVariable long id,@Valid @RequestBody Assign a){orders.assign(id,a.workerId());return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:fulfill')")
    @PostMapping("/orders/{id}/start") public CommonResult<?> start(@PathVariable long id){orders.start(id);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:fulfill')")
    @PostMapping("/orders/{id}/complete") public CommonResult<?> complete(@PathVariable long id){orders.complete(id);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:cancel')")
    @PostMapping("/orders/{id}/cancel") public CommonResult<?> cancel(@PathVariable long id){orders.cancel(id,true);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:reschedule')")
    @PostMapping("/orders/{id}/reschedule") public CommonResult<?> reschedule(@PathVariable long id,@Valid @RequestBody OrderService.Reschedule request){orders.reschedule(id,request,true);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:read')")
    @GetMapping("/orders/{id}/evidence") public CommonResult<?> evidence(@PathVariable long id){return success(workers.adminEvidence(id));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:orders:read')")
    @GetMapping("/orders/{id}/evidence/{evidenceId}/content") public org.springframework.http.ResponseEntity<byte[]> evidenceContent(@PathVariable long id,@PathVariable long evidenceId){return workers.content(id,evidenceId,false,true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:workers:bind')")
    @PostMapping("/workers/{id}/account") public CommonResult<?> workerAccount(@PathVariable long id,@Valid @RequestBody WorkerService.Binding binding){workers.bind(id,binding);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:read')")
    @GetMapping("/workers/{id}/calendar") public CommonResult<?> calendar(@PathVariable long id,@RequestParam LocalDate from,@RequestParam LocalDate to){return success(schedules.calendar(id,from,to));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:write')")
    @PutMapping("/workers/{id}/skills") public CommonResult<?> skills(@PathVariable long id,@Valid @RequestBody ScheduleService.Skills skills){schedules.skills(id,skills);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:write')")
    @PostMapping("/workers/{id}/schedules") public CommonResult<?> schedule(@PathVariable long id,@Valid @RequestBody ScheduleService.Interval interval){return success(schedules.add(id,interval,false));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:write')")
    @DeleteMapping("/workers/{id}/schedules/{scheduleId}") public CommonResult<?> removeSchedule(@PathVariable long id,@PathVariable long scheduleId){schedules.remove(id,scheduleId,false);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:write')")
    @PostMapping("/workers/{id}/leaves") public CommonResult<?> leave(@PathVariable long id,@Valid @RequestBody ScheduleService.Interval interval){return success(schedules.add(id,interval,true));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:write')")
    @DeleteMapping("/workers/{id}/leaves/{leaveId}") public CommonResult<?> removeLeave(@PathVariable long id,@PathVariable long leaveId){schedules.remove(id,leaveId,true);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:read')")
    @GetMapping("/shift-templates") public CommonResult<?> templates(){return success(schedules.templates());}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:templates')")
    @PostMapping("/shift-templates") public CommonResult<?> template(@Valid @RequestBody ScheduleService.Shift shift){return success(schedules.template(shift));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:schedule:write')")
    @PostMapping("/workers/{id}/apply-shift") public CommonResult<?> applyShift(@PathVariable long id,@Valid @RequestBody ScheduleService.ApplyShift request){schedules.apply(id,request);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:aftersales:read')")
    @GetMapping("/aftersales") public CommonResult<?> aftersales(){return success(repo.jdbc().queryForList("SELECT a.*,o.payment_method FROM hm_aftersale a JOIN hm_order o ON o.id=a.order_id AND o.tenant_id=a.tenant_id WHERE a.tenant_id=?"+repo.scope("hm_aftersale","a")+" ORDER BY a.id DESC LIMIT 100",repo.tenant()));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:reviews:read')")
    @GetMapping("/reviews") public CommonResult<?> reviews(){return success(repo.jdbc().queryForList("SELECT id,order_id,rating,content,visible,created_at FROM hm_review WHERE tenant_id=?"+repo.scope("hm_review")+" ORDER BY id DESC LIMIT 100",repo.tenant()));}
    public record Visibility(boolean visible){}
    @PreAuthorize("@hmAdmin.allowed('homemaking:reviews:moderate')")
    @PutMapping("/reviews/{id}/visibility") public CommonResult<?> visibility(@PathVariable long id,@RequestBody Visibility visibility){repo.require("hm_review",id,false);check(repo.jdbc().update("UPDATE hm_review SET visible=? WHERE tenant_id=? AND id=?",visibility.visible(),repo.tenant(),id)==1,"评价不存在");return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:aftersales:refund')")
    @PostMapping("/aftersales/{id}/approve") public CommonResult<?> approve(@PathVariable long id,HttpServletRequest request){payments.approveRefund(id,request.getRemoteAddr());return success(true);}
    public record Reject(@NotBlank @Size(max=1000) String remark){}
    @PreAuthorize("@hmAdmin.allowed('homemaking:aftersales:reject')")
    @PostMapping("/aftersales/{id}/reject") public CommonResult<?> reject(@PathVariable long id,@Valid @RequestBody Reject request){payments.rejectRefund(id,request.remark());return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:aftersales:refund')")
    @PostMapping("/aftersales/{id}/sync") public CommonResult<?> sync(@PathVariable long id){payments.syncRefund(id);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:read')")
    @GetMapping("/settlements") public CommonResult<?> settlements(){return success(repo.jdbc().queryForList("SELECT * FROM hm_settlement WHERE tenant_id=?"+repo.scope("hm_settlement")+" ORDER BY id DESC LIMIT 100",repo.tenant()));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:read') and @hmAdmin.tenant(#tenantId)")
    @GetMapping("/commission-rule/{tenantId}") public CommonResult<?> commissionRule(@PathVariable long tenantId){return success(settlementService.rule(tenantId));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:platform:manage')")
    @PutMapping("/commission-rule/{tenantId}") public CommonResult<?> commissionRule(@PathVariable long tenantId,@Valid @RequestBody SettlementService.Rule rule){settlementService.saveRule(tenantId,rule);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:statement')")
    @PostMapping("/settlement-statements") public CommonResult<?> statement(@Valid @RequestBody SettlementService.Statement statement){return success(settlementService.statement(statement));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:read')")
    @GetMapping("/settlement-statements") public CommonResult<?> statements(@RequestParam long tenantId){return success(settlementService.statements(tenantId));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:approve')")
    @PostMapping("/settlement-statements/{id}/approve") public CommonResult<?> approveStatement(@PathVariable long id){settlementService.approve(id);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:payout')")
    @PostMapping("/settlement-statements/{id}/paid") public CommonResult<?> paidStatement(@PathVariable long id,@Valid @RequestBody SettlementService.Payout payout){settlementService.paid(id,payout);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:finance:reconcile')")
    @PostMapping("/settlement-statements/{id}/reconcile") public CommonResult<?> reconcileStatement(@PathVariable long id){settlementService.reconcile(id);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:customers:read')")
    @GetMapping("/customers") public CommonResult<?> customers(){return success(repo.jdbc().queryForList("SELECT c.id,c.nickname,c.avatar,r.status,r.source,r.created_at FROM hm_customer_tenant r JOIN hm_customer c ON c.id=r.customer_id WHERE r.tenant_id=?"+repo.scope("hm_customer_tenant","r")+" ORDER BY r.created_at DESC LIMIT 100",repo.tenant()));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:brand:read')")
    @GetMapping("/brand") public CommonResult<?> brand(){return success(branding.current());}
    @PreAuthorize("@hmAdmin.allowed('homemaking:brand:write')")
    @PutMapping("/brand") public CommonResult<?> brand(@Valid @RequestBody BrandingService.Brand brand){branding.save(brand);return success(true);}
    public record Domain(@NotBlank @Size(max=253) String domain){}
    @PreAuthorize("@hmAdmin.allowed('homemaking:brand:write')")
    @PostMapping("/domains") public CommonResult<?> domain(@Valid @RequestBody Domain domain){return success(branding.requestDomain(domain.domain()));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:brand:write')")
    @PostMapping("/domains/verify") public CommonResult<?> verify(@Valid @RequestBody Domain domain){branding.verifyDomain(domain.domain());return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:notification:read')")
    @GetMapping("/notifications") public CommonResult<?> notifications(){return success(repo.jdbc().queryForList("SELECT id,customer_id,order_id,event_type,level,status,attempts,next_attempt_at,sent_channel,last_error,created_at FROM hm_notification_outbox WHERE tenant_id=?"+repo.scope("hm_notification_outbox")+" ORDER BY id DESC LIMIT 100",repo.tenant()));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:notification:read')")
    @GetMapping("/notification-policy") public CommonResult<?> policy(@RequestParam(defaultValue="*") String event){return success(Map.of("platform",notifications.policy(0,event),"tenant",notifications.policy(repo.tenant(),event)));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:notification:write') and (!#platform or @hmAdmin.allowed('homemaking:platform:manage'))")
    @PutMapping("/notification-policy") public CommonResult<?> policy(@RequestParam String event,@RequestParam(defaultValue="false") boolean platform,@RequestBody NotificationPolicy policy){notifications.savePolicy(event,policy,platform);return success(true);}
    public record Template(@Pattern(regexp="[A-Z_]{1,60}") String event,@Pattern(regexp="MP|MINI|SMS") String channel,@NotBlank @Size(max=200) String templateId,@NotNull Map<String,String> fieldMapping,boolean enabled){}
    @PreAuthorize("@hmAdmin.allowed('homemaking:notification:write')")
    @PutMapping("/notification-template") public CommonResult<?> template(@Valid @RequestBody Template template) throws Exception {
        String mapping=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(template.fieldMapping());check(mapping.length()<=4000,"模板映射过长");
        repo.jdbc().update("INSERT INTO hm_notification_template(tenant_id,event_type,channel,template_id,field_mapping,enabled) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE template_id=VALUES(template_id),field_mapping=VALUES(field_mapping),enabled=VALUES(enabled)",repo.tenant(),template.event(),template.channel(),template.templateId(),mapping,template.enabled());return success(true);
    }
    public record WechatApp(@Min(1) long tenantId,@Pattern(regexp="wx[A-Za-z0-9]{10,32}") String appId,@Pattern(regexp="MINI|MP") String kind,@NotBlank @Size(max=100) String platformId,@Pattern(regexp="HM_WECHAT_(MINI|MP)(_[A-Z0-9]+)*_SECRET") String secretEnv,boolean enabled){}
    @PreAuthorize("@hmAdmin.allowed('homemaking:platform:manage')")
    @PostMapping("/wechat-apps") public CommonResult<?> app(@Valid @RequestBody WechatApp app){com.hm.module.homemaking.security.AdminScope.platformOnly();quotas.feature(app.tenantId(),app.kind().equals("MINI")?"mini":"mp");check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=? AND deleted=FALSE",Long.class,app.tenantId())==1,"租户不存在");
        repo.jdbc().update("INSERT INTO hm_wechat_app(tenant_id,app_id,kind,platform_id,secret_env,enabled) VALUES(?,?,?,?,?,?)",app.tenantId(),app.appId(),app.kind(),app.platformId(),app.secretEnv(),app.enabled());return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:quota:read') and (#tenantId == null or @hmAdmin.tenant(#tenantId))")
    @GetMapping("/quota") public CommonResult<?> quota(@RequestParam(required=false) Long tenantId){return success(tenantId==null?quotas.current():quotas.describe(tenantId));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:platform:manage')")
    @PutMapping("/quota/{tenantId}") public CommonResult<?> quota(@PathVariable long tenantId,@Valid @RequestBody QuotaService.Entitlement entitlement){quotas.save(tenantId,entitlement);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:platform:manage')")
    @GetMapping("/plans") public CommonResult<?> plans(){return success(quotas.plans());}
    @PreAuthorize("@hmAdmin.allowed('homemaking:platform:manage')")
    @PostMapping("/plans") public CommonResult<?> plan(@Valid @RequestBody QuotaService.Plan plan){return success(quotas.plan(plan));}
    @PreAuthorize("@hmAdmin.allowed('homemaking:portal:read')")
    @GetMapping("/portal") public CommonResult<?> portal(){return success(portal.draft());}
    @PreAuthorize("@hmAdmin.allowed('homemaking:portal:write')")
    @PutMapping("/portal") public CommonResult<?> portal(@Valid @RequestBody PortalService.Draft draft){portal.save(draft);return success(true);}
    @PreAuthorize("@hmAdmin.allowed('homemaking:portal:publish')")
    @PostMapping("/portal/publish") public CommonResult<?> publishPortal(@Valid @RequestBody PortalService.Publish publish){portal.publish(publish);return success(true);}
}
