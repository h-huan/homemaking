package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.framework.tenant.core.util.TenantUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Map;
import static com.hm.module.homemaking.dal.HmRepository.number;

@Component
public class HomemakingJobs {
    private final HmRepository repo;private final NotificationService notifications;private final PaymentService payments;private final org.springframework.context.ApplicationContext context;
    public HomemakingJobs(HmRepository repo,NotificationService notifications,PaymentService payments,org.springframework.context.ApplicationContext context){this.repo=repo;this.notifications=notifications;this.payments=payments;this.context=context;}
    @Scheduled(initialDelay=60000,fixedDelay=60000)
    public void reminders(){
        var rows=repo.jdbc().queryForList("SELECT o.id,o.tenant_id,o.customer_id,b.starts_at FROM hm_order o JOIN hm_booking b ON b.id=o.booking_id AND b.tenant_id=o.tenant_id JOIN system_tenant t ON t.id=o.tenant_id WHERE o.status IN ('PAID','ASSIGNED') AND b.starts_at>? AND b.starts_at<=? AND t.status=0 AND t.deleted=FALSE AND t.expire_time>CURRENT_TIMESTAMP ORDER BY b.starts_at LIMIT 200",LocalDateTime.now(),LocalDateTime.now().plusHours(2));
        for(var row:rows)TenantUtils.execute(number(row,"tenant_id"),()->notifications.enqueue(number(row,"customer_id"),number(row,"id"),"SERVICE_REMINDER","IMPORTANT","reminder:"+row.get("id")+":"+row.get("starts_at"),Map.of("orderId",row.get("id"),"startsAt",row.get("starts_at").toString())));
    }
    @Scheduled(initialDelay=90000,fixedDelay=60000)
    public void refunds(){
        var rows=repo.jdbc().queryForList("SELECT tenant_id,id FROM hm_aftersale WHERE status='REFUNDING' AND pay_refund_id IS NOT NULL ORDER BY id LIMIT 100");
        for(var row:rows){try{TenantUtils.execute(number(row,"tenant_id"),()->payments.syncRefund(number(row,"id")));}catch(Exception ignored){/* Keep the pending record for the next reconciliation pass and operator review. */}}
    }
    @Scheduled(initialDelay=120000,fixedDelay=60000)
    public void expireUnpaid(){
        var rows=repo.jdbc().queryForList("SELECT tenant_id,id FROM hm_order WHERE status='UNPAID' AND payment_method='ONLINE' AND payment_expires_at<? ORDER BY id LIMIT 200",LocalDateTime.now());
        for(var row:rows){try{TenantUtils.execute(number(row,"tenant_id"),()->context.getBean(OrderService.class).expire(number(row,"id")));}catch(Exception ignored){/* A payment callback may win; the next reconciliation pass rechecks state. */}}
    }
}
