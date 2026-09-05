package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.controller.BusinessTimeDeserializer;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmAftersaleService")
public class AftersaleService {
    private static final Set<String> REFUNDS=Set.of("PARTIAL_REFUND","FULL_REFUND");
    private static final Set<String> REMEDIES=Set.of("REWORK","REASSIGN_WORKER","REVISIT");
    public record Request(@NotNull Long orderId,@NotBlank @Pattern(regexp="REWORK|REASSIGN_WORKER|REVISIT|PARTIAL_REFUND|FULL_REFUND|OTHER_COMPENSATION") String type,
                          @Min(1) Integer amountCents,@NotBlank @Size(max=1000) String reason,@NotBlank @Size(max=100) String requestKey) {}
    public record Schedule(@NotNull @Future @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime startsAt,Long workerId,
                           @NotBlank @Size(max=1000) String resolution) {}
    public record Resolve(@NotBlank @Size(max=1000) String remark) {}
    private final HmRepository repo;private final CustomerAccess customers;private final ScheduleService schedules;private final NotificationService notifications;
    public AftersaleService(HmRepository repo,CustomerAccess customers,ScheduleService schedules,NotificationService notifications){this.repo=repo;this.customers=customers;this.schedules=schedules;this.notifications=notifications;}

    @Transactional public long apply(Request request){
        long tenant=repo.tenant(),customer=customers.current();
        var existing=repo.jdbc().queryForList("SELECT id FROM hm_aftersale WHERE tenant_id=? AND customer_id=? AND request_key=?",tenant,customer,request.requestKey());
        if(!existing.isEmpty())return number(existing.get(0),"id");
        var order=customers.own("hm_order",request.orderId(),true);check(order.get("pending_change_id")==null,"订单变更处理期间不能申请售后");
        String type=request.type();boolean waiting="IN_SERVICE".equals(order.get("status"))&&"AWAITING_CONFIRMATION".equals(order.get("fulfillment_status"));
        if(REMEDIES.contains(type))check(waiting||"COMPLETED".equals(order.get("status"))||type.equals("REASSIGN_WORKER")&&"ASSIGNED".equals(order.get("status")),"当前履约阶段不能申请该服务补救");
        else check(waiting||Set.of("PAID","ASSIGNED","COMPLETED").contains(order.get("status")),"当前订单不能申请售后");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND status IN ('REQUESTED','SCHEDULED','IN_PROGRESS','AWAITING_CONFIRMATION','REFUNDING')",Long.class,tenant,request.orderId())==0,"已有售后申请正在处理");
        Integer amount=null;int remaining=cents(order,"paid_cents")-cents(order,"refunded_cents");
        if(type.equals("PARTIAL_REFUND")){check(request.amountCents()!=null&&request.amountCents()>0&&request.amountCents()<remaining,"部分退款金额须小于剩余可退金额");amount=request.amountCents();}
        else if(type.equals("FULL_REFUND")){check(remaining>0,"订单已无可退金额");amount=remaining;}
        else check(request.amountCents()==null,"服务补救和其他补偿不填写退款金额");
        long id=repo.insert("INSERT INTO hm_aftersale(tenant_id,customer_id,order_id,amount_cents,reason,type,request_key) VALUES(?,?,?,?,?,?,?)",tenant,customer,request.orderId(),amount,request.reason().trim(),type,request.requestKey());
        log(id,request.orderId(),"APPLIED",null,"REQUESTED",request.reason());orderLog(request.orderId(),"AFTERSALE_REQUESTED",type+"："+request.reason());notify(order,id,"REQUESTED");return id;
    }
    public Map<String,Object> customerDetail(long id){var a=repo.require("hm_aftersale",id,false);check(number(a,"customer_id")==customers.current(),"售后单不属于当前客户");return enrich(a);}
    public List<Map<String,Object>> byOrder(long order,boolean admin){if(admin)repo.require("hm_order",order,false);else customers.own("hm_order",order,false);return enrich(repo.jdbc().queryForList("SELECT * FROM hm_aftersale WHERE tenant_id=? AND order_id=? ORDER BY id",repo.tenant(),order));}
    public Map<String,Object> adminDetail(long id){return enrich(repo.require("hm_aftersale",id,false));}
    private List<Map<String,Object>> enrich(List<Map<String,Object>> rows){for(var row:rows)enrich(row);return rows;}
    private Map<String,Object> enrich(Map<String,Object> row){row.put("logs",repo.jdbc().queryForList("SELECT action,from_status,to_status,actor_id,actor_type,detail,created_at FROM hm_aftersale_log WHERE tenant_id=? AND aftersale_id=? ORDER BY id",repo.tenant(),row.get("id")));return row;}

    @Transactional public void cancel(long id,Resolve request){var a=repo.require("hm_aftersale",id,true);check(number(a,"customer_id")==customers.current(),"售后单不属于当前客户");check("REQUESTED".equals(a.get("status"))&&a.get("order_change_id")==null,"当前售后不能撤销");move(a,"CANCELLED","CUSTOMER_CANCELLED",request.remark(),true);orderLog(number(a,"order_id"),"AFTERSALE_CANCELLED",request.remark());}
    @Transactional public void reject(long id,Resolve request){var a=repo.require("hm_aftersale",id,true);check("REQUESTED".equals(a.get("status"))&&a.get("order_change_id")==null,"当前售后不能驳回");move(a,"REJECTED","REJECTED",request.remark(),true);repo.jdbc().update("UPDATE hm_aftersale SET audit_remark=? WHERE tenant_id=? AND id=?",request.remark().trim(),repo.tenant(),id);orderLog(number(a,"order_id"),"AFTERSALE_REJECTED",request.remark());}
    @Transactional public void resolve(long id,Resolve request){var a=repo.require("hm_aftersale",id,true);check("OTHER_COMPENSATION".equals(a.get("type"))&&"REQUESTED".equals(a.get("status")),"当前售后不能登记补偿结果");move(a,"COMPLETED","COMPENSATION_COMPLETED",request.remark(),true);orderLog(number(a,"order_id"),"AFTERSALE_COMPLETED",request.remark());}
    public void generated(long id,long order,String type,String detail){repo.jdbc().update("UPDATE hm_aftersale SET type=? WHERE tenant_id=? AND id=?",type,repo.tenant(),id);log(id,order,"SYSTEM_CREATED",null,"REQUESTED",detail);orderLog(order,"AFTERSALE_REQUESTED",type+"："+detail);}
    @Transactional public void cancelGenerated(long changeId,String reason){var rows=repo.jdbc().queryForList("SELECT * FROM hm_aftersale WHERE tenant_id=? AND order_change_id=? AND status='REQUESTED' FOR UPDATE",repo.tenant(),changeId);for(var a:rows){repo.jdbc().update("UPDATE hm_aftersale SET status='REJECTED',audit_remark=?,resolution=?,processed_at=CURRENT_TIMESTAMP,closed_at=CURRENT_TIMESTAMP,version=version+1 WHERE tenant_id=? AND id=?",reason,reason,repo.tenant(),a.get("id"));log(number(a,"id"),number(a,"order_id"),"CHANGE_CANCELLED","REQUESTED","REJECTED",reason);}}
    @Transactional public void schedule(long id,Schedule request){
        var a=repo.require("hm_aftersale",id,true);String type=Objects.toString(a.get("type"));check(REMEDIES.contains(type)&&"REQUESTED".equals(a.get("status")),"当前售后不能安排服务补救");
        var order=repo.require("hm_order",number(a,"order_id"),true);var oldBooking=repo.require("hm_booking",number(order,"booking_id"),true);
        long minutes=Duration.between(OrderService.time(oldBooking.get("starts_at")),OrderService.time(oldBooking.get("ends_at"))).toMinutes();LocalDateTime end=request.startsAt().plusMinutes(minutes);
        long worker=schedules.select(number(order,"service_id"),request.workerId(),Objects.toString(order.get("district_code"),""),request.startsAt(),end,null);
        if(type.equals("REASSIGN_WORKER")&&order.get("worker_id")!=null)check(worker!=number(order,"worker_id"),"换服务人员必须选择不同人员");
        repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),oldBooking.get("id"));
        repo.jdbc().update("UPDATE hm_booking SET status='AFTERSALE_REPLACED' WHERE tenant_id=? AND id=?",repo.tenant(),oldBooking.get("id"));
        long booking=repo.insert("INSERT INTO hm_booking(tenant_id,customer_id,service_id,store_id,worker_id,starts_at,ends_at,status,aftersale_id) VALUES(?,?,?,?,?,?,?,'RESERVED',?)",repo.tenant(),order.get("customer_id"),order.get("service_id"),order.get("store_id"),worker,request.startsAt(),end,id);
        for(var t=request.startsAt();t.isBefore(end);t=t.plusMinutes(30))repo.jdbc().update("INSERT INTO hm_worker_slot VALUES(?,?,?,?)",repo.tenant(),worker,t,booking);
        repo.jdbc().update("UPDATE hm_order SET booking_id=?,worker_id=?,status='ASSIGNED',fulfillment_status='WAITING',worker_completed_at=NULL,confirmation_deadline=NULL,completion_confirmed_at=NULL,completion_method=NULL,completion_confirmed_by=NULL,completed_at=NULL,version=version+1 WHERE tenant_id=? AND id=?",booking,worker,repo.tenant(),order.get("id"));
        repo.jdbc().update("UPDATE hm_aftersale SET status='SCHEDULED',resolution=?,assigned_worker_id=?,remedy_booking_id=?,scheduled_at=?,processed_at=CURRENT_TIMESTAMP,version=version+1 WHERE tenant_id=? AND id=?",request.resolution().trim(),worker,booking,request.startsAt(),repo.tenant(),id);
        log(id,number(order,"id"),"SCHEDULED","REQUESTED","SCHEDULED",request.resolution());orderLog(number(order,"id"),"AFTERSALE_SCHEDULED",type+"，预约="+request.startsAt());notify(order,id,"SCHEDULED");
    }
    @Transactional public void workerAction(long orderId,String action){
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND remedy_booking_id=(SELECT booking_id FROM hm_order WHERE tenant_id=? AND id=?) AND status IN ('SCHEDULED','IN_PROGRESS','AWAITING_CONFIRMATION') FOR UPDATE",repo.tenant(),orderId,repo.tenant(),orderId);if(rows.isEmpty())return;var a=rows.get(0);
        if(action.equals("REJECT")){repo.jdbc().update("UPDATE hm_booking SET status='CANCELLED' WHERE tenant_id=? AND id=?",repo.tenant(),a.get("remedy_booking_id"));repo.jdbc().update("UPDATE hm_aftersale SET status='REQUESTED',assigned_worker_id=NULL,remedy_booking_id=NULL,scheduled_at=NULL,version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),a.get("id"));log(number(a,"id"),orderId,"WORKER_REJECTED",Objects.toString(a.get("status")),"REQUESTED","服务人员拒绝，等待重新安排");}
        else if(action.equals("START")&&"SCHEDULED".equals(a.get("status")))move(a,"IN_PROGRESS","REMEDY_STARTED","服务补救已开始",false);
        else if(action.equals("COMPLETE")&&Set.of("SCHEDULED","IN_PROGRESS").contains(a.get("status")))move(a,"AWAITING_CONFIRMATION","REMEDY_SUBMITTED","服务补救已提交，等待客户确认",false);
    }
    @Transactional public void completionConfirmed(long orderId){var rows=repo.jdbc().queryForList("SELECT * FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND status='AWAITING_CONFIRMATION' FOR UPDATE",repo.tenant(),orderId);for(var a:rows)move(a,"COMPLETED","REMEDY_CONFIRMED","客户或系统已确认服务补救完成",true);}
    @Transactional public void refundStarted(long id){var a=repo.require("hm_aftersale",id,false);check(REFUNDS.contains(a.get("type")),"该售后不是退款申请");log(id,number(a,"order_id"),"REFUND_STARTED","REQUESTED","REFUNDING","退款已提交");}
    @Transactional public void refundFailed(long id){var a=repo.require("hm_aftersale",id,false);log(id,number(a,"order_id"),"REFUND_FAILED","REFUNDING","FAILED","退款失败，可核对后重新处理");}
    @Transactional public void refundRetried(long id,String detail){var a=repo.require("hm_aftersale",id,false);log(id,number(a,"order_id"),"REFUND_RETRIED","FAILED","REQUESTED",detail);notify(repo.require("hm_order",number(a,"order_id"),false),id,"REQUESTED");}
    @Transactional public void refundCompleted(long id){var a=repo.require("hm_aftersale",id,false);log(id,number(a,"order_id"),"REFUND_COMPLETED",Objects.toString(a.get("status")),"REFUNDED","退款已完成");repo.jdbc().update("UPDATE hm_aftersale SET processed_at=COALESCE(processed_at,CURRENT_TIMESTAMP),closed_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",repo.tenant(),id);}
    private void move(Map<String,Object> a,String to,String action,String detail,boolean close){String from=Objects.toString(a.get("status"));repo.jdbc().update("UPDATE hm_aftersale SET status=?,resolution=CASE WHEN ?='' THEN resolution ELSE ? END,processed_at=COALESCE(processed_at,CURRENT_TIMESTAMP),closed_at=CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE closed_at END,version=version+1 WHERE tenant_id=? AND id=?",to,detail.trim(),detail.trim(),close,repo.tenant(),a.get("id"));log(number(a,"id"),number(a,"order_id"),action,from,to,detail);notify(repo.require("hm_order",number(a,"order_id"),false),number(a,"id"),to);}
    private void log(long id,long order,String action,String from,String to,String detail){var user=SecurityFrameworkUtils.getLoginUser();repo.insert("INSERT INTO hm_aftersale_log(tenant_id,aftersale_id,order_id,action,from_status,to_status,actor_id,actor_type,detail) VALUES(?,?,?,?,?,?,?,?,?)",repo.tenant(),id,order,action,from,to,user==null?null:user.getId(),user==null?null:user.getUserType(),CatalogService.s(detail));}
    private void orderLog(long order,String action,String detail){repo.insert("INSERT INTO hm_order_log(tenant_id,order_id,action,actor_id,detail) VALUES(?,?,?,?,?)",repo.tenant(),order,action,SecurityFrameworkUtils.getLoginUserId(),CatalogService.s(detail));}
    private void notify(Map<String,Object> order,long id,String status){notifications.enqueue(number(order,"customer_id"),number(order,"id"),"AFTERSALE_UPDATE","IMPORTANT","aftersale:"+id+":"+status,Map.of("orderId",order.get("id"),"aftersaleId",id,"status",status));}
}
