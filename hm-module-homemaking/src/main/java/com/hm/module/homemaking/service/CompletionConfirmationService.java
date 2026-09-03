package com.hm.module.homemaking.service;

import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.dal.HmRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service
public class CompletionConfirmationService {
    private final HmRepository repo; private final CustomerAccess customers; private final SettlementService settlements; private final NotificationService notifications;
    public CompletionConfirmationService(HmRepository repo,CustomerAccess customers,SettlementService settlements,NotificationService notifications){this.repo=repo;this.customers=customers;this.settlements=settlements;this.notifications=notifications;}
    @Transactional public void submit(long id){
        var order=repo.require("hm_order",id,true);check("IN_SERVICE".equals(order.get("status"))&&"STARTED".equals(order.get("fulfillment_status")),"服务尚未按履约流程开始");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_fulfillment_evidence WHERE tenant_id=? AND order_id=? AND worker_id=? AND phase='AFTER'",Long.class,repo.tenant(),id,order.get("worker_id"))>0,"缺少服务后凭证");
        int hours=repo.jdbc().queryForObject("SELECT completion_confirm_hours FROM hm_tenant_profile WHERE tenant_id=?",Integer.class,repo.tenant());check(hours>=1&&hours<=168,"客户确认期限配置无效");
        var deadline=LocalDateTime.now().plusHours(hours);
        repo.jdbc().update("UPDATE hm_order SET worker_completed_at=CURRENT_TIMESTAMP,confirmation_deadline=?,fulfillment_status='AWAITING_CONFIRMATION',version=version+1 WHERE tenant_id=? AND id=?",deadline,repo.tenant(),id);
        repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),order.get("booking_id"));repo.jdbc().update("UPDATE hm_booking SET status='AWAITING_CONFIRMATION' WHERE tenant_id=? AND id=?",repo.tenant(),order.get("booking_id"));
        log(id,"WORKER_COMPLETION_SUBMITTED","等待客户确认，截止 "+deadline);
        notifications.enqueue(number(order,"customer_id"),id,"SERVICE_COMPLETION_CONFIRM","IMPORTANT","completion-confirm:"+id,Map.of("orderId",id,"deadline",deadline.toString()));
    }
    @Transactional public void confirmByCustomer(long id){var order=customers.own("hm_order",id,true);confirmLocked(order,"CUSTOMER",customers.current());}
    @Transactional public void confirmTimeout(long tenant,long id){var previous=TenantContextHolder.getTenantId();TenantContextHolder.setTenantId(tenant);try{var order=repo.require("hm_order",id,true);if("AWAITING_CONFIRMATION".equals(order.get("fulfillment_status"))&&OrderService.time(order.get("confirmation_deadline")).isBefore(LocalDateTime.now().plusSeconds(1)))confirmLocked(order,"TIMEOUT",null);}finally{if(previous!=null)TenantContextHolder.setTenantId(previous);else TenantContextHolder.clear();}}
    private void confirmLocked(Map<String,Object> order,String method,Long actor){
        if("COMPLETED".equals(order.get("status")))return;
        check("IN_SERVICE".equals(order.get("status"))&&"AWAITING_CONFIRMATION".equals(order.get("fulfillment_status")),"当前订单不等待完工确认");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND status IN ('REQUESTED','REFUNDING')",Long.class,repo.tenant(),order.get("id"))==0,"售后处理中，处理完成前不能确认完工");
        long id=number(order,"id");repo.jdbc().update("UPDATE hm_order SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP,completion_confirmed_at=CURRENT_TIMESTAMP,completion_method=?,completion_confirmed_by=?,fulfillment_status='COMPLETED',version=version+1 WHERE tenant_id=? AND id=?",method,actor,repo.tenant(),id);
        repo.jdbc().update("UPDATE hm_booking SET status='COMPLETED' WHERE tenant_id=? AND id=?",repo.tenant(),order.get("booking_id"));
        var completed=repo.require("hm_order",id,false);settlements.completed(completed);repo.insert("INSERT INTO hm_order_log(tenant_id,order_id,action,actor_id,detail) VALUES(?,?,?,?,?)",repo.tenant(),id,"COMPLETION_CONFIRMED",actor,method);
        notifications.enqueue(number(order,"customer_id"),id,"SERVICE_COMPLETED","NORMAL","complete:"+id,Map.of("orderId",id,"method",method));
    }
    @Transactional @Scheduled(fixedDelay=60000) public void timeouts(){
        var previous=TenantContextHolder.getTenantId();try{TenantContextHolder.setIgnore(true);var due=repo.jdbc().queryForList("SELECT o.tenant_id,o.id FROM hm_order o WHERE o.status='IN_SERVICE' AND o.fulfillment_status='AWAITING_CONFIRMATION' AND o.confirmation_deadline<=CURRENT_TIMESTAMP AND NOT EXISTS(SELECT 1 FROM hm_aftersale a WHERE a.tenant_id=o.tenant_id AND a.order_id=o.id AND a.status IN ('REQUESTED','REFUNDING')) ORDER BY o.confirmation_deadline LIMIT 200");TenantContextHolder.setIgnore(false);for(var row:due)confirmTimeout(number(row,"tenant_id"),number(row,"id"));}finally{TenantContextHolder.setIgnore(false);if(previous!=null)TenantContextHolder.setTenantId(previous);else TenantContextHolder.clear();}
    }
    private void log(long order,String action,String detail){repo.insert("INSERT INTO hm_order_log(tenant_id,order_id,action,actor_id,detail) VALUES(?,?,?,?,?)",repo.tenant(),order,action,SecurityFrameworkUtils.getLoginUserId(),detail);}
}
