package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmOrderService")
public class OrderService {
    public record Book(@NotNull Long serviceId, Long workerId, @NotNull @Future LocalDateTime startsAt,
            @NotNull Long addressId, @NotBlank @Size(max=100) String requestKey,Long skuId,List<PricingService.Extra> extras,@Size(max=500) String customerRemark) {
        public Book(Long serviceId,Long workerId,LocalDateTime startsAt,Long addressId,String requestKey){this(serviceId,workerId,startsAt,addressId,requestKey,null,List.of(),"");}
    }
    public record Address(Long id,@NotBlank @Size(max=100) String contactName,@NotBlank @Size(max=32) String phone,
            @NotBlank @Size(max=500) String address,boolean isDefault) {}
    public record Review(@NotNull Long orderId,@Min(1) @Max(5) int rating,@NotBlank @Size(max=1000) String content) {}
    public record Aftersale(@NotNull Long orderId,@Min(1) int amountCents,@NotBlank @Size(max=1000) String reason) {}
    private final HmRepository repo;
    private final CustomerAccess customers;
    private final NotificationService notifications;
    private final PricingService pricing;
    public OrderService(HmRepository repo,CustomerAccess customers,NotificationService notifications,PricingService pricing){this.repo=repo;this.customers=customers;this.notifications=notifications;this.pricing=pricing;}
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
        var service=repo.require("hm_service",request.serviceId(),true);
        check("ACTIVE".equals(service.get("status")),"服务已下架");
        var store=repo.require("hm_store",number(service,"store_id"),false);check("ACTIVE".equals(store.get("status")),"门店不可用");
        var address=customers.own("hm_customer_address",request.addressId(),false);
        var quote=pricing.quote(request.serviceId(),request.skuId(),request.extras(),request.addressId());
        LocalDateTime start=request.startsAt(),end=start.plusMinutes(quote.durationMinutes());
        pricing.validateTime(request.serviceId(),start);
        check(start.getMinute()%30==0&&start.getSecond()==0&&start.getNano()==0,"请选择半小时预约时段");
        check(start.getHour()>=8&&end.toLocalDate().equals(start.toLocalDate())&&!end.toLocalTime().isAfter(LocalTime.of(21,0)),"服务时间须在 08:00–21:00");
        if(request.workerId()!=null)validateWorker(request.workerId(),number(service,"store_id"));
        long booking=repo.insert("INSERT INTO hm_booking(tenant_id,customer_id,service_id,store_id,worker_id,starts_at,ends_at) VALUES(?,?,?,?,?,?,?)",tenant,customer,request.serviceId(),service.get("store_id"),request.workerId(),start,end);
        if(request.workerId()!=null)reserveSlots(booking,request.workerId(),start,end);
        long order=repo.insert("INSERT INTO hm_order(tenant_id,customer_id,booking_id,service_id,service_name,store_id,worker_id,price_cents,contact_name,phone,address,request_key) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                tenant,customer,booking,request.serviceId(),service.get("name"),service.get("store_id"),request.workerId(),quote.totalCents(),address.get("contact_name"),address.get("phone"),address.get("address"),request.requestKey());
        repo.jdbc().update("UPDATE hm_order SET customer_remark=? WHERE tenant_id=? AND id=?",CatalogService.s(request.customerRemark()),tenant,order);
        for(var item:quote.items())repo.insert("INSERT INTO hm_order_item(tenant_id,order_id,name,price_cents,quantity,total_cents) VALUES(?,?,?,?,?,?)",tenant,order,item.name(),item.priceCents(),item.quantity(),item.totalCents());
        log(order,"CREATED","");return order;
    }
    void validateWorker(long worker,long store){var w=repo.require("hm_worker",worker,true);check(number(w,"store_id")==store&&"ACTIVE".equals(w.get("status")),"服务人员不属于当前门店或不可用");}
    void reserveSlots(long booking,long worker,LocalDateTime start,LocalDateTime end){
        for(LocalDateTime at=start;at.isBefore(end);at=at.plusMinutes(30))
            repo.jdbc().update("INSERT INTO hm_worker_slot(tenant_id,worker_id,starts_at,booking_id) VALUES(?,?,?,?)",repo.tenant(),worker,at,booking);
    }
    public Map<String,Object> detail(long id,boolean admin){var order=admin?repo.require("hm_order",id,false):customers.own("hm_order",id,false);order.put("booking",repo.require("hm_booking",number(order,"booking_id"),false));return order;}
    public Map<String,Object> list(int page,int size,boolean admin){
        size=Math.min(100,Math.max(1,size));page=Math.max(1,page);
        String where=" WHERE tenant_id=?"+(admin?"":" AND customer_id=?");var args=new ArrayList<Object>();args.add(repo.tenant());if(!admin)args.add(customers.current());
        long total=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_order"+where,Long.class,args.toArray());args.add(size);args.add((page-1)*size);
        return Map.of("list",repo.jdbc().queryForList("SELECT * FROM hm_order"+where+" ORDER BY id DESC LIMIT ? OFFSET ?",args.toArray()),"total",total);
    }
    @Transactional
    public void cancel(long id,boolean admin){var order=admin?repo.require("hm_order",id,true):customers.own("hm_order",id,true);
        if("CANCELLED".equals(order.get("status")))return;
        transition(order,"CANCELLED");release(order,"CANCELLED");log(id,"CANCELLED","");
        notifications.enqueue(number(order,"customer_id"),id,"ORDER_CANCELLED","IMPORTANT","cancel:"+id,Map.of("orderId",id));
    }
    @Transactional
    public void assign(long id,long worker){var order=repo.require("hm_order",id,true);check(Set.of("PAID","ASSIGNED").contains(order.get("status")),"只有已付款订单可派单");validateWorker(worker,number(order,"store_id"));
        var booking=repo.require("hm_booking",number(order,"booking_id"),true);
        repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),booking.get("id"));
        reserveSlots(number(booking,"id"),worker,time(booking.get("starts_at")),time(booking.get("ends_at")));
        repo.jdbc().update("UPDATE hm_booking SET worker_id=? WHERE tenant_id=? AND id=?",worker,repo.tenant(),booking.get("id"));
        repo.jdbc().update("UPDATE hm_order SET worker_id=?,status='ASSIGNED',version=version+1 WHERE tenant_id=? AND id=?",worker,repo.tenant(),id);
        log(id,"ASSIGNED","worker="+worker);notifications.enqueue(number(order,"customer_id"),id,"WORKER_CHANGED","IMPORTANT","assign:"+id+":"+(number(order,"version")+1),Map.of("orderId",id));
    }
    @Transactional
    public void start(long id){var order=repo.require("hm_order",id,true);transition(order,"IN_SERVICE");log(id,"STARTED","");}
    @Transactional
    public void complete(long id){var order=repo.require("hm_order",id,true);transition(order,"COMPLETED");repo.jdbc().update("UPDATE hm_order SET completed_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",repo.tenant(),id);release(order,"COMPLETED");
        int net=cents(order,"paid_cents")-cents(order,"refunded_cents");
        repo.insert("INSERT INTO hm_settlement(tenant_id,order_id,store_id,gross_cents,refund_cents,net_cents) VALUES(?,?,?,?,?,?)",repo.tenant(),id,order.get("store_id"),order.get("paid_cents"),order.get("refunded_cents"),net);
        log(id,"COMPLETED","");notifications.enqueue(number(order,"customer_id"),id,"SERVICE_COMPLETED","NORMAL","complete:"+id,Map.of("orderId",id));
    }
    @Transactional
    public long review(Review r){var order=customers.own("hm_order",r.orderId(),true);check("COMPLETED".equals(order.get("status")),"完工后才能评价");return repo.insert("INSERT INTO hm_review(tenant_id,customer_id,order_id,rating,content) VALUES(?,?,?,?,?)",repo.tenant(),customers.current(),r.orderId(),r.rating(),r.content());}
    @Transactional
    public long aftersale(Aftersale a){var order=customers.own("hm_order",a.orderId(),true);check(Set.of("PAID","ASSIGNED","COMPLETED").contains(order.get("status")),"当前状态不可申请退款");
        check(a.amountCents()<=cents(order,"paid_cents")-cents(order,"refunded_cents"),"退款金额超过可退金额");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND status IN ('REQUESTED','REFUNDING')",Long.class,repo.tenant(),a.orderId())==0,"已有售后申请正在处理");
        return repo.insert("INSERT INTO hm_aftersale(tenant_id,customer_id,order_id,amount_cents,reason) VALUES(?,?,?,?,?)",repo.tenant(),customers.current(),a.orderId(),a.amountCents(),a.reason());
    }
    void transition(Map<String,Object> order,String to){check(OrderState.allows((String)order.get("status"),to),"订单状态不允许此操作");int changed=repo.jdbc().update("UPDATE hm_order SET status=?,version=version+1 WHERE tenant_id=? AND id=? AND version=?",to,repo.tenant(),order.get("id"),order.get("version"));check(changed==1,"订单已更新，请重试");}
    void release(Map<String,Object> order,String status){repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),order.get("booking_id"));repo.jdbc().update("UPDATE hm_booking SET status=? WHERE tenant_id=? AND id=?",status,repo.tenant(),order.get("booking_id"));}
    void log(long order,String action,String detail){repo.insert("INSERT INTO hm_order_log(tenant_id,order_id,action,actor_id,detail) VALUES(?,?,?,?,?)",repo.tenant(),order,action,SecurityFrameworkUtils.getLoginUserId(),detail);}
    public static LocalDateTime time(Object value){return value instanceof LocalDateTime l?l:((Timestamp)value).toLocalDateTime();}
}
