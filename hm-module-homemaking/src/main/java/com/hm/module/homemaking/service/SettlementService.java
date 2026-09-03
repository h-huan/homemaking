package com.hm.module.homemaking.service;

import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import com.hm.module.homemaking.security.AdminScope;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmSettlementService")
public class SettlementService {
    public record Rule(@Min(0) @Max(10000) int platformBps,@Min(0) @Max(10000) int workerBps,
                       @Min(1) @Max(90) int cycleDays,boolean enabled,@Min(0) long version) {}
    public record Statement(@Min(1) long tenantId,@NotNull @Pattern(regexp="STORE|WORKER") String beneficiary,
                            @Min(1) long beneficiaryId,@NotNull LocalDate periodStart,@NotNull LocalDate periodEnd) {}
    public record Payout(@NotBlank @Size(max=150) String reference) {}
    private final HmRepository repo;private final QuotaService quotas;
    public SettlementService(HmRepository repo,QuotaService quotas){this.repo=repo;this.quotas=quotas;}
    public Map<String,Object> rule(long tenant){AdminScope.tenant(tenant);check(repo.tenant()==tenant||repo.tenant()==1,"无权查看该规则");var rows=repo.jdbc().queryForList("SELECT * FROM hm_commission_rule WHERE tenant_id=?",tenant);return rows.isEmpty()?Map.of("tenant_id",tenant,"platform_bps",0,"worker_bps",0,"cycle_days",30,"enabled",false,"version",0):rows.get(0);}
    @Transactional public void saveRule(long tenant,Rule r){
        AdminScope.platformOnly();check(repo.tenant()==1,"仅总部可设置平台抽成和人员分成规则");check(r.platformBps()+r.workerBps()<=10000,"平台与人员分成合计不能超过 100%");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=? AND deleted=FALSE",Long.class,tenant)==1,"租户不存在");
        var current=repo.jdbc().queryForList("SELECT version FROM hm_commission_rule WHERE tenant_id=? FOR UPDATE",tenant);
        if(current.isEmpty()){
            check(r.version()==0,"规则已更新，请刷新");
            repo.jdbc().update("INSERT INTO hm_commission_rule(tenant_id,platform_bps,worker_bps,cycle_days,enabled,version) VALUES(?,?,?,?,?,0)",tenant,r.platformBps(),r.workerBps(),r.cycleDays(),r.enabled());
        }else{
            check(number(current.get(0),"version")==r.version(),"规则已更新，请刷新");
            repo.jdbc().update("UPDATE hm_commission_rule SET platform_bps=?,worker_bps=?,cycle_days=?,enabled=?,version=version+1 WHERE tenant_id=?",r.platformBps(),r.workerBps(),r.cycleDays(),r.enabled(),tenant);
        }
    }
    @Transactional public void completed(Map<String,Object> order){
        long tenant=repo.tenant(),id=number(order,"id");int gross=cents(order,"paid_cents"),refund=cents(order,"refunded_cents"),net=gross-refund;
        repo.jdbc().update("INSERT INTO hm_settlement(tenant_id,order_id,store_id,gross_cents,refund_cents,net_cents) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE refund_cents=VALUES(refund_cents),net_cents=VALUES(net_cents)",tenant,id,order.get("store_id"),gross,refund,net);
        var settings=rule(tenant);if(!Boolean.TRUE.equals(settings.get("enabled")))return;
        quotas.feature("settlement");int platform=cents(settings,"platform_bps"),worker=cents(settings,"worker_bps");
        long platformAmount=(long)net*platform/10000,workerAmount=order.get("worker_id")==null?0:(long)net*worker/10000,storeAmount=net-platformAmount-workerAmount;
        repo.jdbc().update("INSERT INTO hm_settlement_allocation(tenant_id,order_id,store_id,worker_id,platform_bps,worker_bps,net_cents,platform_cents,worker_cents,store_cents) VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE order_id=VALUES(order_id)",tenant,id,order.get("store_id"),order.get("worker_id"),platform,worker,net,platformAmount,workerAmount,storeAmount);
        entry(order,"PLATFORM",1,platformAmount,"complete:"+id);entry(order,"STORE",number(order,"store_id"),storeAmount,"complete:"+id);
        if(order.get("worker_id")!=null&&workerAmount!=0)entry(order,"WORKER",number(order,"worker_id"),workerAmount,"complete:"+id);
    }
    @Transactional public void refund(Map<String,Object> order,long aftersale,int amount){
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_settlement_allocation WHERE tenant_id=? AND order_id=? FOR UPDATE",repo.tenant(),order.get("id"));if(rows.isEmpty())return;var allocation=rows.get(0);
        if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_settlement_entry WHERE tenant_id=? AND event_key=?",Long.class,repo.tenant(),"refund:"+aftersale)>0)return;
        long net=number(allocation,"net_cents")-amount;check(amount>0&&net>=0,"结算冲正金额无效");
        long platform=number(allocation,"platform_cents")-net*cents(allocation,"platform_bps")/10000;
        long worker=number(allocation,"worker_cents")-(allocation.get("worker_id")==null?0:net*cents(allocation,"worker_bps")/10000),store=amount-platform-worker;
        entry(order,"PLATFORM",1,-platform,"refund:"+aftersale);entry(order,"STORE",number(order,"store_id"),-store,"refund:"+aftersale);
        if(allocation.get("worker_id")!=null&&worker!=0)entry(order,"WORKER",number(allocation,"worker_id"),-worker,"refund:"+aftersale);
        repo.jdbc().update("UPDATE hm_settlement_allocation SET net_cents=net_cents-?,platform_cents=platform_cents-?,worker_cents=worker_cents-?,store_cents=store_cents-? WHERE tenant_id=? AND order_id=?",amount,platform,worker,store,repo.tenant(),order.get("id"));
    }
    private void entry(Map<String,Object> order,String type,long beneficiary,long amount,String event){if(amount==0)return;repo.jdbc().update("INSERT INTO hm_settlement_entry(tenant_id,order_id,beneficiary,beneficiary_id,amount_cents,event_key) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE event_key=VALUES(event_key)",repo.tenant(),order.get("id"),type,beneficiary,amount,event);}
    @Transactional public long statement(Statement s){
        AdminScope.tenant(s.tenantId());check(s.tenantId()==repo.tenant()||repo.tenant()==1,"无权生成该租户结算单");check(!s.periodEnd().isBefore(s.periodStart())&&s.periodEnd().isBefore(s.periodStart().plusDays(93)),"结算周期最多 93 天");
        if(s.beneficiary().equals("STORE"))check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_store WHERE tenant_id=? AND id=?"+repo.scope("hm_store"),Long.class,s.tenantId(),s.beneficiaryId())==1,"门店不存在");
        else check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker WHERE tenant_id=? AND id=?"+repo.scope("hm_worker"),Long.class,s.tenantId(),s.beneficiaryId())==1,"人员不存在");
        var entries=repo.jdbc().queryForList("SELECT id,amount_cents FROM hm_settlement_entry WHERE tenant_id=? AND beneficiary=? AND beneficiary_id=? AND statement_id IS NULL AND created_at>=? AND created_at<? ORDER BY id FOR UPDATE",s.tenantId(),s.beneficiary(),s.beneficiaryId(),s.periodStart().atStartOfDay(),s.periodEnd().plusDays(1).atStartOfDay());check(!entries.isEmpty(),"没有未结算明细");
        long amount=entries.stream().mapToLong(e->number(e,"amount_cents")).sum();long id=repo.insert("INSERT INTO hm_settlement_statement(tenant_id,beneficiary,beneficiary_id,period_start,period_end,amount_cents,created_by) VALUES(?,?,?,?,?,?,?)",s.tenantId(),s.beneficiary(),s.beneficiaryId(),s.periodStart(),s.periodEnd(),amount,SecurityFrameworkUtils.getLoginUserId());
        for(var entry:entries)check(repo.jdbc().update("UPDATE hm_settlement_entry SET statement_id=? WHERE id=? AND statement_id IS NULL",id,entry.get("id"))==1,"结算明细已被处理");return id;
    }
    public List<Map<String,Object>> statements(long tenant){AdminScope.tenant(tenant);check(tenant==repo.tenant()||repo.tenant()==1,"无权查看结算单");return repo.jdbc().queryForList("SELECT * FROM hm_settlement_statement WHERE tenant_id=?"+repo.scope("hm_settlement_statement")+" ORDER BY id DESC LIMIT 200",tenant);}
    @Transactional public void approve(long id){var s=lock(id);check("DRAFT".equals(s.get("status"))&&number(s,"amount_cents")>0,"结算单不可审核或金额非正数");update(id,"APPROVED","approved_by",SecurityFrameworkUtils.getLoginUserId());}
    @Transactional public void paid(long id,Payout p){var s=lock(id);check("APPROVED".equals(s.get("status")),"结算单尚未审核");repo.jdbc().update("UPDATE hm_settlement_statement SET status='PAID',payment_reference=?,paid_by=?,paid_at=CURRENT_TIMESTAMP WHERE id=?",p.reference(),SecurityFrameworkUtils.getLoginUserId(),id);}
    @Transactional public void reconcile(long id){var s=lock(id);check("PAID".equals(s.get("status")),"结算单尚未登记打款");update(id,"RECONCILED","reconciled_by",SecurityFrameworkUtils.getLoginUserId());repo.jdbc().update("UPDATE hm_settlement_statement SET reconciled_at=CURRENT_TIMESTAMP WHERE id=?",id);}
    private Map<String,Object> lock(long id){var scope=AdminScope.current();boolean platform=scope!=null?scope.platform():repo.tenant()==1;var rows=platform?repo.jdbc().queryForList("SELECT * FROM hm_settlement_statement WHERE id=? FOR UPDATE",id):repo.jdbc().queryForList("SELECT * FROM hm_settlement_statement WHERE id=? AND tenant_id=?"+repo.scope("hm_settlement_statement")+" FOR UPDATE",id,repo.tenant());check(rows.size()==1,"结算单不存在");return rows.get(0);}
    private void update(long id,String status,String actor,Object value){repo.jdbc().update("UPDATE hm_settlement_statement SET status=?,"+actor+"=? WHERE id=?",status,value,id);}
}
