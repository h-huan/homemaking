package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.framework.tenant.core.util.TenantUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmNotificationService")
public class NotificationService {
    private final HmRepository repo;private final ObjectMapper json;private final TransactionTemplate tx;private final NotificationGateway gateway;
    @Value("${hm.homemaking.notification-delivery-enabled:false}") private boolean deliveryEnabled;
    public NotificationService(HmRepository repo,ObjectMapper json,PlatformTransactionManager manager,NotificationGateway gateway){this.repo=repo;this.json=json;this.tx=new TransactionTemplate(manager);this.gateway=gateway;}
    public void enqueue(long customer,Long order,String event,String level,String dedup,Map<String,Object> payload){
        try{repo.jdbc().update("INSERT INTO hm_notification_outbox(tenant_id,customer_id,order_id,event_type,level,dedup_key,payload) VALUES(?,?,?,?,?,?,?)",repo.tenant(),customer,order,event,level,dedup,json.writeValueAsString(payload));}
        catch(DuplicateKeyException ignored){/* Same logical event is already durably queued. */}
        catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("Invalid notification data",e);}
    }
    public NotificationPolicy policy(long tenant,String event){
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_notification_policy WHERE tenant_id=? AND event_type IN (?, '*') ORDER BY CASE WHEN event_type='*' THEN 1 ELSE 0 END LIMIT 1",tenant,event);
        if(rows.isEmpty())return NotificationPolicy.defaults();var r=rows.get(0);
        return new NotificationPolicy(bool(r.get("enabled")),cents(r,"daily_limit"),cents(r,"min_interval_minutes"),cents(r,"quiet_start"),cents(r,"quiet_end"),bool(r.get("allow_sms")),Arrays.asList(((String)r.get("channels")).split(",")));
    }
    private static boolean bool(Object o){return o instanceof Boolean b?b:((Number)o).intValue()!=0;}
    @Scheduled(fixedDelayString="${hm.homemaking.notification-poll-ms:15000}")
    public void dispatch(){if(!deliveryEnabled)return;
        // A crashed or timed-out send has an unknown provider outcome. Never blindly resend it.
        repo.jdbc().update("UPDATE hm_notification_outbox SET status='UNKNOWN',last_error='发送结果待核实' WHERE status='SENDING' AND locked_until<CURRENT_TIMESTAMP");
        for(int i=0;i<20;i++){
            var job=tx.execute(status->reserve());if(job==null)return;
            var result=TenantUtils.execute(number(job,"tenant_id"),()->gateway.send(job));
            tx.executeWithoutResult(status->{
                String outcome=result.outcome();String next=outcome.equals("REJECTED")?"PENDING":(outcome.equals("UNKNOWN")?"UNKNOWN":"SENT");
                repo.jdbc().update("UPDATE hm_notification_delivery SET status=?,provider_id=? WHERE outbox_id=? AND channel=? AND status='RESERVED'",outcome,result.providerId(),job.get("id"),job.get("channel"));
                repo.jdbc().update("UPDATE hm_notification_outbox SET status=?,sent_channel=?,last_error=?,attempts=attempts+1,locked_until=NULL,next_attempt_at=? WHERE id=? AND lock_token=? AND status='SENDING'",next,job.get("channel"),result.message(),LocalDateTime.now().plusMinutes(1),job.get("id"),job.get("lock_token"));
            });
        }
    }
    private Map<String,Object> reserve(){
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_notification_outbox WHERE status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP ORDER BY id LIMIT 1 FOR UPDATE");if(rows.isEmpty())return null;
        var job=rows.get(0);long tenant=number(job,"tenant_id"),customer=number(job,"customer_id"),id=number(job,"id");
        // One platform-wide lock serializes quota reservations across all tenants for a customer.
        var customers=repo.jdbc().queryForList("SELECT id FROM hm_customer WHERE id=? AND status='ACTIVE' FOR UPDATE",customer);
        if(customers.isEmpty()||repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_customer_tenant WHERE tenant_id=? AND customer_id=? AND status='ACTIVE'",Long.class,tenant,customer)==0){mark(id,"SUPPRESSED","CUSTOMER_DISABLED",LocalDateTime.now());return null;}
        String event=(String)job.get("event_type");var pref=repo.jdbc().queryForList("SELECT * FROM hm_notification_preference WHERE tenant_id=? AND customer_id=? AND event_type=?",tenant,customer,event);
        boolean consent=pref.isEmpty()?!event.equals("MARKETING"):bool(pref.get(0).get("enabled"));boolean sms=!pref.isEmpty()&&bool(pref.get(0).get("allow_sms"))&&!"NORMAL".equals(job.get("level"));
        LocalDateTime now=LocalDateTime.now(ZoneId.of("Asia/Shanghai")),midnight=now.toLocalDate().atStartOfDay();
        String charged=" AND status IN ('RESERVED','SENT','ACCEPTED','UNKNOWN')";
        long all=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_notification_delivery WHERE customer_id=? AND created_at>=?"+charged,Long.class,customer,midnight);
        long local=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_notification_delivery WHERE tenant_id=? AND customer_id=? AND created_at>=?"+charged,Long.class,tenant,customer,midnight);
        var last=repo.jdbc().queryForList("SELECT created_at FROM hm_notification_delivery WHERE customer_id=?"+charged+" ORDER BY id DESC LIMIT 1",customer);
        var lastSent=last.isEmpty()?null:OrderService.time(last.get(0).get("created_at"));
        boolean urgent="URGENT".equals(job.get("level"));
        var global=NotificationPolicy.decide(policy(0,"*"),policy(tenant,"*"),consent,sms,urgent,now,all,local,lastSent);
        var specific=NotificationPolicy.decide(policy(0,event),policy(tenant,event),consent,sms,urgent,now,all,local,lastSent);
        var decision=NotificationPolicy.intersect(global,specific);
        if(!decision.allowed()){mark(id,decision.defer()?"PENDING":"SUPPRESSED",decision.reason(),now.plusMinutes(30));return null;}
        var tried=repo.jdbc().queryForList("SELECT channel FROM hm_notification_delivery WHERE outbox_id=?",String.class,id);
        String channel=decision.channels().stream().filter(c->!tried.contains(c)).findFirst().orElse(null);
        if(channel==null){mark(id,"FAILED","所有允许渠道均不可用",now);return null;}
        String token=UUID.randomUUID().toString();repo.jdbc().update("UPDATE hm_notification_outbox SET status='SENDING',lock_token=?,locked_until=? WHERE id=?",token,now.plusMinutes(5),id);
        repo.insert("INSERT INTO hm_notification_delivery(tenant_id,customer_id,outbox_id,channel,status) VALUES(?,?,?,?,'RESERVED')",tenant,customer,id,channel);
        job.put("channel",channel);job.put("lock_token",token);return job;
    }
    private void mark(long id,String status,String reason,LocalDateTime next){repo.jdbc().update("UPDATE hm_notification_outbox SET status=?,last_error=?,next_attempt_at=? WHERE id=?",status,reason,next,id);}
    public void savePolicy(String event,NotificationPolicy policy,boolean platform){
        check(event.matches("[A-Z_]{1,60}|\\*"),"事件名无效");check(policy.dailyLimit()>=0&&policy.dailyLimit()<=100&&policy.minIntervalMinutes()>=0&&policy.minIntervalMinutes()<=1440&&policy.quietStart()>=0&&policy.quietStart()<24&&policy.quietEnd()>=0&&policy.quietEnd()<24,"策略数值无效");
        check(policy.channels()!=null&&Set.of("MP","MINI","SMS").containsAll(policy.channels()),"通知渠道无效");long tenant=platform?0:repo.tenant();if(platform)check(repo.tenant()==1,"仅总部可设置平台上限");
        repo.jdbc().update("INSERT INTO hm_notification_policy(tenant_id,event_type,enabled,daily_limit,min_interval_minutes,quiet_start,quiet_end,allow_sms,channels) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled),daily_limit=VALUES(daily_limit),min_interval_minutes=VALUES(min_interval_minutes),quiet_start=VALUES(quiet_start),quiet_end=VALUES(quiet_end),allow_sms=VALUES(allow_sms),channels=VALUES(channels)",tenant,event,policy.enabled(),policy.dailyLimit(),policy.minIntervalMinutes(),policy.quietStart(),policy.quietEnd(),policy.allowSms(),String.join(",",policy.channels()));
    }
}
