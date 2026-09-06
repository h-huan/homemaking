package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hm.module.homemaking.controller.BusinessTimeDeserializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmOrderService")
public class OrderService {
    public record Book(@NotNull Long serviceId, Long workerId, @NotNull @Future @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime startsAt,
            @NotNull Long addressId, @NotBlank @Size(max=100) String requestKey,Long skuId,List<PricingService.Extra> extras,@Size(max=500) String customerRemark) {
        public Book(Long serviceId,Long workerId,LocalDateTime startsAt,Long addressId,String requestKey){this(serviceId,workerId,startsAt,addressId,requestKey,null,List.of(),"");}
    }
    public record Address(Long id,@NotBlank @Size(max=100) String contactName,@NotBlank @Size(max=32) String phone,
            @NotBlank @Size(max=500) String address,boolean isDefault) {}
    public record Review(@NotNull Long orderId,@Min(1) @Max(5) int rating,@NotBlank @Size(max=1000) String content) {}
    public record Aftersale(@NotNull Long orderId,@Min(1) int amountCents,@NotBlank @Size(max=1000) String reason) {}
    public record Reschedule(@NotNull @Future @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime startsAt,Long workerId,@NotBlank @Size(max=500) String reason) {}
    private final HmRepository repo;
    private final CustomerAccess customers;
    private final NotificationService notifications;
    private final PricingService pricing;
    private final ScheduleService schedules;
    private final QuotaService quotas;
    private final SettlementService settlements;
    @org.springframework.beans.factory.annotation.Autowired private PaymentPolicyService paymentPolicy;
    @org.springframework.beans.factory.annotation.Autowired private OrderChangeService changes;
    @org.springframework.beans.factory.annotation.Autowired private CompletionConfirmationService completionConfirmation;
    @org.springframework.beans.factory.annotation.Autowired private AftersaleService aftersales;
    public OrderService(HmRepository repo,CustomerAccess customers,NotificationService notifications,PricingService pricing,ScheduleService schedules,QuotaService quotas,SettlementService settlements){this.repo=repo;this.customers=customers;this.notifications=notifications;this.pricing=pricing;this.schedules=schedules;this.quotas=quotas;this.settlements=settlements;}
    @Transactional
    public long saveAddress(Address a) {
        long customer=customers.current(),tenant=repo.tenant();
        if(a.id()!=null)customers.own("hm_customer_address",a.id(),true);
        if(a.isDefault())repo.jdbc().update("UPDATE hm_customer_address SET is_default=FALSE WHERE tenant_id=? AND customer_id=?",tenant,customer);
        if(a.id()==null)return repo.insert("INSERT INTO hm_customer_address(tenant_id,customer_id,contact_name,phone,address,is_default) VALUES(?,?,?,?,?,?)",tenant,customer,a.contactName(),a.phone(),a.address(),a.isDefault());
        repo.jdbc().update("UPDATE hm_customer_address SET contact_name=?,phone=?,address=?,is_default=?,version=version+1 WHERE tenant_id=? AND customer_id=? AND id=?",a.contactName(),a.phone(),a.address(),a.isDefault(),tenant,customer,a.id());return a.id();
    }
    public List<Map<String,Object>> addresses(){return repo.jdbc().queryForList("SELECT * FROM hm_customer_address WHERE tenant_id=? AND customer_id=? ORDER BY is_default DESC,id DESC LIMIT 100",repo.tenant(),customers.current());}
    @Transactional
    public long book(Book request) {
        long customer=customers.current(),tenant=repo.tenant();
        var existing=repo.jdbc().queryForList("SELECT id FROM hm_order WHERE tenant_id=? AND customer_id=? AND request_key=?",tenant,customer,request.requestKey());
        if(!existing.isEmpty())return number(existing.get(0),"id");
        quotas.checkCreate("orders");
        var service=repo.require("hm_service",request.serviceId(),true);
        check("ACTIVE".equals(service.get("status")),"服务已下架");
        var store=repo.require("hm_store",number(service,"store_id"),false);check("ACTIVE".equals(store.get("status")),"门店不可用");
        var address=customers.own("hm_customer_address",request.addressId(),false);
        var quote=pricing.quote(request.serviceId(),request.skuId(),request.extras(),request.addressId());
        LocalDateTime start=request.startsAt(),end=start.plusMinutes(quote.durationMinutes());
        pricing.validateTime(request.serviceId(),start);
        check(start.getMinute()%30==0&&start.getSecond()==0&&start.getNano()==0,"请选择半小时预约时段");
        check(start.getHour()>=8&&end.toLocalDate().equals(start.toLocalDate())&&!end.toLocalTime().isAfter(LocalTime.of(21,0)),"服务时间须在 08:00–21:00");
        long worker=schedules.select(request.serviceId(),request.workerId(),Objects.toString(address.get("district_code"),""),start,end,null);
        long booking=repo.insert("INSERT INTO hm_booking(tenant_id,customer_id,service_id,store_id,worker_id,starts_at,ends_at) VALUES(?,?,?,?,?,?,?)",tenant,customer,request.serviceId(),service.get("store_id"),worker,start,end);
        reserveSlots(booking,worker,start,end);
        long order=repo.insert("INSERT INTO hm_order(tenant_id,customer_id,booking_id,service_id,service_name,store_id,worker_id,price_cents,contact_name,phone,address,request_key) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant,customer,booking,request.serviceId(),service.get("name"),service.get("store_id"),worker,quote.totalCents(),address.get("contact_name"),address.get("phone"),address.get("address"),request.requestKey());
        repo.jdbc().update("UPDATE hm_order SET customer_remark=?,district_code=? WHERE tenant_id=? AND id=?",CatalogService.s(request.customerRemark()),Objects.toString(address.get("district_code"),""),tenant,order);
        repo.jdbc().update("UPDATE hm_order SET area_fee_cents=? WHERE tenant_id=? AND id=?",pricing.areaFee(request.serviceId(),Objects.toString(address.get("district_code"),"")),tenant,order);
        boolean onlineOnly=!paymentPolicy.options().offlineAvailable();
        repo.jdbc().update("UPDATE hm_order SET payment_method=?,payment_expires_at=? WHERE tenant_id=? AND id=?",onlineOnly?"ONLINE":"OFFLINE",onlineOnly?LocalDateTime.now().plusMinutes(30):null,tenant,order);
        for(var item:quote.items())repo.insert("INSERT INTO hm_order_item(tenant_id,order_id,name,price_cents,quantity,total_cents) VALUES(?,?,?,?,?,?)",tenant,order,item.name(),item.priceCents(),item.quantity(),item.totalCents());
        log(order,"CREATED","");return order;
    }
    void validateWorker(long worker,long store){var w=repo.require("hm_worker",worker,true);check(number(w,"store_id")==store&&"ACTIVE".equals(w.get("status")),"服务人员不属于当前门店或不可用");}
    void reserveSlots(long booking,long worker,LocalDateTime start,LocalDateTime end){
        for(LocalDateTime at=start;at.isBefore(end);at=at.plusMinutes(30))
            repo.jdbc().update("INSERT INTO hm_worker_slot(tenant_id,worker_id,starts_at,booking_id) VALUES(?,?,?,?)",repo.tenant(),worker,at,booking);
    }
    public Map<String,Object> detail(long id,boolean admin){var order=admin?repo.require("hm_order",id,false):customers.own("hm_order",id,false);order.put("booking",repo.require("hm_booking",number(order,"booking_id"),false));
        var paymentOptions=paymentPolicy.options();
        var pending=order.get("pending_change_id")==null?null:repo.require("hm_order_change",number(order,"pending_change_id"),false);boolean supplement=pending!=null&&"PENDING_PAYMENT".equals(pending.get("status"));
        boolean waiting="UNPAID".equals(order.get("status"));
        boolean expired=order.get("payment_expires_at")!=null&&!time(order.get("payment_expires_at")).isAfter(LocalDateTime.now());
        order.put("payment_options",Map.of("mode",paymentOptions.mode(),"onlineAvailable",waiting&&!expired&&paymentOptions.onlineAvailable(),"offlineAvailable",(waiting||supplement)&&order.get("pay_order_id")==null&&paymentOptions.offlineAvailable(),"message",order.get("pay_order_id")!=null&&waiting?"线上支付结果待核对，请勿重复线下付款":paymentOptions.message()));
        var history=changes.history(id);order.put("changes",history);order.put("pending_change",history.stream().filter(c->Objects.equals(c.get("id"),order.get("pending_change_id"))).findFirst().orElse(null));
        order.put("logs",repo.jdbc().queryForList("SELECT id,action,detail,created_at FROM hm_order_log WHERE tenant_id=? AND order_id=? ORDER BY id",repo.tenant(),id));
        order.put("aftersales",aftersales.byOrder(id,admin));
        order.put("review",repo.jdbc().queryForList("SELECT id,rating,service_rating,worker_rating,tags_json,content,reply_content,replied_at FROM hm_review WHERE tenant_id=? AND order_id=? AND status='PUBLISHED'",repo.tenant(),id));return order;}
    public Map<String,Object> list(int page,int size,boolean admin){
        size=Math.min(100,Math.max(1,size));page=Math.max(1,page);
        String where=" WHERE hm_order.tenant_id=?"+(admin?repo.scope("hm_order"):" AND hm_order.customer_id=?");var args=new ArrayList<Object>();args.add(repo.tenant());if(!admin)args.add(customers.current());
        long total=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_order"+where,Long.class,args.toArray());args.add(size);args.add((page-1)*size);
        return Map.of("list",repo.jdbc().queryForList("SELECT hm_order.*,c.difference_cents AS pending_difference_cents,c.status AS change_status FROM hm_order LEFT JOIN hm_order_change c ON c.tenant_id=hm_order.tenant_id AND c.id=hm_order.pending_change_id"+where+" ORDER BY hm_order.id DESC LIMIT ? OFFSET ?",args.toArray()),"total",total);
    }
    @Transactional
    public void cancel(long id,boolean admin){var order=admin?repo.require("hm_order",id,true):customers.own("hm_order",id,true);
        if("CANCELLED".equals(order.get("status")))return;changes.requireSettled(order);
        if(Set.of("PAID","ASSIGNED").contains(order.get("status"))){
            var booking=repo.require("hm_booking",number(order,"booking_id"),false);
            if(!admin)pricing.validateChange(number(order,"service_id"),time(booking.get("starts_at")),false);
            check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND status IN ('REQUESTED','SCHEDULED','IN_PROGRESS','AWAITING_CONFIRMATION','REFUNDING')",Long.class,repo.tenant(),id)==0,"已有售后申请正在处理");
            int remaining=cents(order,"paid_cents")-cents(order,"refunded_cents");check(remaining>0,"没有可退款金额");
            long aftersale=repo.insert("INSERT INTO hm_aftersale(tenant_id,customer_id,order_id,amount_cents,reason,type) VALUES(?,?,?,?,?,'FULL_REFUND')",repo.tenant(),order.get("customer_id"),id,remaining,"取消预约退款");aftersales.generated(aftersale,id,"FULL_REFUND","取消预约退款");
            repo.jdbc().update("UPDATE hm_order SET status='CANCELLED',version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),id);
        }else transition(order,"CANCELLED");
        release(order,"CANCELLED");log(id,"CANCELLED","");
        notifications.enqueue(number(order,"customer_id"),id,"ORDER_CANCELLED","IMPORTANT","cancel:"+id,Map.of("orderId",id));
    }
    @Transactional
    public void assign(long id,long worker){var order=repo.require("hm_order",id,true);changes.requireSettled(order);check(Set.of("PAID","ASSIGNED").contains(order.get("status")),"只有已付款订单可派单");validateWorker(worker,number(order,"store_id"));
        var booking=repo.require("hm_booking",number(order,"booking_id"),true);
        check(Set.of("WAITING","ACCEPTED").contains(order.get("fulfillment_status")),"人员已经到达，请先处理当前履约");
        schedules.select(number(order,"service_id"),worker,Objects.toString(order.get("district_code"),""),time(booking.get("starts_at")),time(booking.get("ends_at")),number(booking,"id"));
        repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),booking.get("id"));
        reserveSlots(number(booking,"id"),worker,time(booking.get("starts_at")),time(booking.get("ends_at")));
        repo.jdbc().update("UPDATE hm_booking SET worker_id=? WHERE tenant_id=? AND id=?",worker,repo.tenant(),booking.get("id"));
        repo.jdbc().update("UPDATE hm_order SET worker_id=?,status='ASSIGNED',fulfillment_status='WAITING',version=version+1 WHERE tenant_id=? AND id=?",worker,repo.tenant(),id);
        log(id,"ASSIGNED","worker="+worker);notifications.enqueue(number(order,"customer_id"),id,"WORKER_CHANGED","IMPORTANT","assign:"+id+":"+(number(order,"version")+1),Map.of("orderId",id));
    }
    @Transactional
    public void start(long id){var order=repo.require("hm_order",id,true);changes.requireSettled(order);check("ARRIVED".equals(order.get("fulfillment_status")),"人员尚未到达打卡");check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_fulfillment_evidence WHERE tenant_id=? AND order_id=? AND worker_id=? AND phase='BEFORE'",Long.class,repo.tenant(),id,order.get("worker_id"))>0,"缺少服务前凭证");transition(order,"IN_SERVICE");repo.jdbc().update("UPDATE hm_order SET fulfillment_status='STARTED' WHERE tenant_id=? AND id=?",repo.tenant(),id);log(id,"STARTED","");}
    @Transactional
    public void reschedule(long id,Reschedule request,boolean admin){
        var order=admin?repo.require("hm_order",id,true):customers.own("hm_order",id,true);
        changes.requireSettled(order);check(Set.of("UNPAID","PAID","ASSIGNED").contains(order.get("status")),"当前状态不允许改约");
        check(Set.of("WAITING","ACCEPTED").contains(order.get("fulfillment_status")),"人员已经到达，请联系门店处理");
        var booking=repo.require("hm_booking",number(order,"booking_id"),true);
        var before=changes.snapshot(order,booking);
        if(!admin){pricing.validateChange(number(order,"service_id"),time(booking.get("starts_at")),true);check(number(order,"reschedule_count")<2,"自助改约已达两次，请联系门店");}
        LocalDateTime start=request.startsAt(),end=start.plusMinutes(Duration.between(time(booking.get("starts_at")),time(booking.get("ends_at"))).toMinutes());
        pricing.validateTime(number(order,"service_id"),start);
        check(start.getHour()>=8&&end.toLocalDate().equals(start.toLocalDate())&&!end.toLocalTime().isAfter(LocalTime.of(21,0)),"服务须在 08:00–21:00 完成");
        long worker=schedules.select(number(order,"service_id"),request.workerId(),Objects.toString(order.get("district_code"),""),start,end,number(booking,"id"));
        repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),booking.get("id"));
        reserveSlots(number(booking,"id"),worker,start,end);
        repo.jdbc().update("UPDATE hm_booking SET starts_at=?,ends_at=?,worker_id=? WHERE tenant_id=? AND id=?",start,end,worker,repo.tenant(),booking.get("id"));
        repo.jdbc().update("UPDATE hm_order SET worker_id=?,fulfillment_status='WAITING',reschedule_count=reschedule_count+1,version=version+1 WHERE tenant_id=? AND id=?",worker,repo.tenant(),id);
        var after=changes.snapshot(repo.require("hm_order",id,false),repo.require("hm_booking",number(booking,"id"),false));changes.recordReschedule(order,before,after,request.reason());
        log(id,"RESCHEDULED",start+" / "+request.reason());
        notifications.enqueue(number(order,"customer_id"),id,"WORKER_CHANGED","IMPORTANT","reschedule:"+id+":"+(number(order,"version")+1),Map.of("orderId",id));
    }
    @Transactional public void expire(long id){
        var order=repo.require("hm_order",id,true);
        if(!"UNPAID".equals(order.get("status"))||!"ONLINE".equals(order.get("payment_method"))||order.get("payment_expires_at")==null||time(order.get("payment_expires_at")).isAfter(LocalDateTime.now()))return;
        transition(order,"CANCELLED");release(order,"CANCELLED");log(id,"PAYMENT_EXPIRED","30 分钟未支付，释放预约");
    }
    @Transactional
    public void complete(long id){changes.requireSettled(repo.require("hm_order",id,false));completionConfirmation.submit(id);}
    @Transactional
    public long aftersale(Aftersale a){var order=customers.own("hm_order",a.orderId(),false);int remaining=cents(order,"paid_cents")-cents(order,"refunded_cents");String type=a.amountCents()==remaining?"FULL_REFUND":"PARTIAL_REFUND";return aftersales.apply(new AftersaleService.Request(a.orderId(),type,a.amountCents(),a.reason(),"legacy-"+UUID.randomUUID()));}
    void transition(Map<String,Object> order,String to){check(OrderState.allows((String)order.get("status"),to),"订单状态不允许此操作");int changed=repo.jdbc().update("UPDATE hm_order SET status=?,version=version+1 WHERE tenant_id=? AND id=? AND version=?",to,repo.tenant(),order.get("id"),order.get("version"));check(changed==1,"订单已更新，请重试");}
    void release(Map<String,Object> order,String status){repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),order.get("booking_id"));repo.jdbc().update("UPDATE hm_booking SET status=? WHERE tenant_id=? AND id=?",status,repo.tenant(),order.get("booking_id"));}
    void log(long order,String action,String detail){repo.insert("INSERT INTO hm_order_log(tenant_id,order_id,action,actor_id,detail) VALUES(?,?,?,?,?)",repo.tenant(),order,action,SecurityFrameworkUtils.getLoginUserId(),detail);}
    public static LocalDateTime time(Object value){return value instanceof LocalDateTime l?l:((Timestamp)value).toLocalDateTime();}
}
