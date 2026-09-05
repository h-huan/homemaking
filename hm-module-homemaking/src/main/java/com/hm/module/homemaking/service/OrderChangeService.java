package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.AdminScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

/** Financial amendments retain the original contract until the difference is settled. */
@Service
public class OrderChangeService {
    @org.springframework.beans.factory.annotation.Autowired private AftersaleService aftersales;
    public record Contact(@NotBlank @Size(max=100) String contactName,@NotBlank @Size(max=32) String phone,
                          @NotBlank @Size(max=500) String address,@NotNull @Size(max=20) String districtCode) {}
    public record Request(@Min(0) long version,@Valid Contact contact,@Min(1) @Max(100000000) Integer priceCents,
                          @NotBlank @Size(max=500) String reason,@NotNull @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String requestKey,
                          @Min(1) @Max(100000000) Integer expectedPriceCents) {
        public Request(long version,Contact contact,Integer priceCents,String reason,String requestKey){this(version,contact,priceCents,reason,requestKey,priceCents);}
    }
    public record CustomerRequest(@Min(0) long version,@Min(1) long addressId,@NotBlank @Size(max=500) String reason,
                                  @NotNull @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String requestKey,@Min(1) @Max(100000000) Integer expectedPriceCents) {
        public CustomerRequest(long version,long addressId,String reason,String key){this(version,addressId,reason,key,null);}
    }
    public record Cancel(@NotBlank @Size(max=500) String reason) {}
    private final HmRepository repo; private final PricingService pricing; private final ScheduleService schedules;
    private final CustomerAccess customers; private final PaymentPolicyService policy; private final ObjectMapper json;
    public OrderChangeService(HmRepository repo,PricingService pricing,ScheduleService schedules,CustomerAccess customers,PaymentPolicyService policy,ObjectMapper json){
        this.repo=repo;this.pricing=pricing;this.schedules=schedules;this.customers=customers;this.policy=policy;this.json=json;
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalArgumentException("订单快照保存失败",e);}}
    private Map<String,Object> decode(Object value){try{return json.readValue(value.toString(),new TypeReference<>(){});}catch(JsonProcessingException e){throw new IllegalArgumentException("订单快照读取失败",e);}}
    private String fingerprint(Request r){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encode(r).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private long actor(){var user=SecurityFrameworkUtils.getLoginUser();AdminScope.denyUnless(user!=null);return user.getId();}
    private int actorType(){return SecurityFrameworkUtils.getLoginUser().getUserType();}
    private Map<String,Object> owned(long id,boolean admin,boolean lock){
        if(admin)AdminScope.denyUnless(actorType()==2&&AdminScope.current()!=null);
        return admin?repo.require("hm_order",id,lock):customers.own("hm_order",id,lock);
    }
    public void requireSettled(Map<String,Object> order){check(order.get("pending_change_id")==null,"订单变更差额尚未处理，请先结清或撤销变更");}
    public Map<String,Object> snapshot(Map<String,Object> order,Map<String,Object> booking){
        var result=new LinkedHashMap<String,Object>();
        for(String key:List.of("contact_name","phone","address","district_code","price_cents","area_fee_cents","worker_id"))result.put(key,order.get(key));
        result.put("starts_at",OrderService.time(booking.get("starts_at")).toString());result.put("ends_at",OrderService.time(booking.get("ends_at")).toString());return result;
    }
    private void editable(Map<String,Object> order,boolean admin){
        requireSettled(order);
        check(Set.of("UNPAID","PAID","ASSIGNED").contains(order.get("status"))&&Set.of("WAITING","ACCEPTED").contains(order.get("fulfillment_status")),"当前履约状态不能变更订单");
        var booking=repo.require("hm_booking",number(order,"booking_id"),false);
        check(OrderService.time(booking.get("ends_at")).isAfter(LocalDateTime.now()),"预约时段已结束，请先改期");
        if(!admin)pricing.validateChange(number(order,"service_id"),OrderService.time(booking.get("starts_at")),true);
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND (status IN ('REQUESTED','SCHEDULED','IN_PROGRESS','AWAITING_CONFIRMATION','REFUNDING') OR (order_change_id IS NULL AND status='REFUNDED'))",Long.class,repo.tenant(),order.get("id"))==0,"请先处理进行中的售后；已发生售后退款的订单不能重新改价");
    }
    private Map<String,Object> proposed(Map<String,Object> order,Request r,boolean canPrice){
        var booking=repo.require("hm_booking",number(order,"booking_id"),false);var after=snapshot(order,booking);
        if(r.priceCents()!=null)AdminScope.denyUnless(canPrice);
        long amount=cents(order,"price_cents");
        if(r.contact()!=null){
            var c=r.contact();boolean changedArea=!c.districtCode().equals(order.get("district_code"));
            int fee=pricing.areaFee(number(order,"service_id"),c.districtCode());
            if(changedArea){
                check(order.get("area_fee_cents")!=null||r.priceCents()!=null,"历史区域报价信息缺失，请门店确认新的订单总价");
                amount=amount-(order.get("area_fee_cents")==null?0:cents(order,"area_fee_cents"))+fee;after.put("area_fee_cents",fee);
            }
            after.put("contact_name",c.contactName());after.put("phone",c.phone());after.put("address",c.address());after.put("district_code",c.districtCode());
        }
        if(r.priceCents()!=null)amount=r.priceCents();
        check(amount>0&&amount<=100000000&&(after.get("area_fee_cents")==null||amount>number(after,"area_fee_cents")),"新的订单金额须大于零且覆盖区域服务费");
        after.put("price_cents",(int)amount);
        validateCoverage(order,after);
        int difference=(int)amount-cents(order,"price_cents");
        if(difference!=0){
            check(order.get("pay_order_id")==null&&!"LEGACY".equals(order.get("payment_method")),"已有线上支付记录或历史未对账订单不能直接改价；请先原路退款并重新预约");
            if(cents(order,"paid_cents")>0){
                check("OFFLINE".equals(order.get("payment_method"))&&cents(order,"paid_cents")-cents(order,"refunded_cents")==cents(order,"price_cents"),"原订单收支未结清，请先对账");
                if(difference>0)check(policy.options().offlineAvailable(),"请先开启线下收款能力，再办理已付订单补差额");
            }
        }
        return after;
    }
    private void validateCoverage(Map<String,Object> order,Map<String,Object> after){
        check(LocalDateTime.parse(after.get("ends_at").toString()).isAfter(LocalDateTime.now()),"预约时段已结束，请撤销变更并先改期");
        pricing.areaFee(number(order,"service_id"),after.get("district_code").toString());
        check(order.get("worker_id")!=null,"请先分配可服务该地址的人员");
        schedules.select(number(order,"service_id"),number(order,"worker_id"),after.get("district_code").toString(),LocalDateTime.parse(after.get("starts_at").toString()),LocalDateTime.parse(after.get("ends_at").toString()),number(order,"booking_id"));
    }
    public Map<String,Object> preview(long id,Request r,boolean admin,boolean canPrice){
        var order=owned(id,admin,false);editable(order,admin);check(number(order,"version")==r.version(),"订单已更新，请刷新后重新确认");
        var after=proposed(order,r,canPrice);int delta=cents(after,"price_cents")-cents(order,"price_cents");
        return Map.of("before",snapshot(order,repo.require("hm_booking",number(order,"booking_id"),false)),"after",after,"differenceCents",delta,"requiresSettlement",cents(order,"paid_cents")>0&&delta!=0);
    }
    @Transactional public long create(long id,Request r,boolean admin,boolean canPrice){
        var order=owned(id,admin,true);String hash=fingerprint(r);
        var repeat=repo.jdbc().queryForList("SELECT * FROM hm_order_change WHERE tenant_id=? AND order_id=? AND request_key=?",repo.tenant(),id,r.requestKey());
        if(!repeat.isEmpty()){var previous=repeat.get(0);check(hash.equals(previous.get("request_hash"))&&number(previous,"actor_id")==actor()&&number(previous,"actor_type")==actorType(),"请求编号已用于其他变更");return number(previous,"id");}
        editable(order,admin);check(number(order,"version")==r.version(),"订单已更新，请刷新后重新确认");
        var before=snapshot(order,repo.require("hm_booking",number(order,"booking_id"),false));var after=proposed(order,r,canPrice);
        check(r.expectedPriceCents()!=null&&r.expectedPriceCents()==cents(after,"price_cents"),"报价已变化或尚未核对，请重新核对后确认");
        check(!encode(before).equals(encode(after)),"订单内容未改变");int delta=cents(after,"price_cents")-cents(order,"price_cents");
        String status=cents(order,"paid_cents")==0||delta==0?"APPLIED":delta>0?"PENDING_PAYMENT":"PENDING_REFUND";
        long change=record(id,"AMENDMENT",status,before,after,r.reason(),r.requestKey(),hash);
        if(status.equals("APPLIED"))apply(order,change,after);
        else{
            repo.jdbc().update("UPDATE hm_order SET pending_change_id=?,version=version+1 WHERE tenant_id=? AND id=?",change,repo.tenant(),id);
            if(delta<0){long aftersale=repo.insert("INSERT INTO hm_aftersale(tenant_id,customer_id,order_id,amount_cents,reason,type,order_change_id) VALUES(?,?,?,?,?,'PARTIAL_REFUND',?)",repo.tenant(),order.get("customer_id"),id,-delta,"订单变更退差额："+r.reason(),change);aftersales.generated(aftersale,id,"PARTIAL_REFUND","订单变更退差额："+r.reason());}
        }
        log(id,"CHANGE_REQUESTED","变更单 #"+change+"："+r.reason());return change;
    }
    public Request customerRequest(long id,CustomerRequest r){
        customers.own("hm_order",id,false);var a=customers.own("hm_customer_address",r.addressId(),false);
        return new Request(r.version(),new Contact(a.get("contact_name").toString(),a.get("phone").toString(),a.get("address").toString(),a.get("district_code").toString()),null,r.reason(),r.requestKey(),r.expectedPriceCents());
    }
    private long record(long id,String kind,String status,Map<String,Object> before,Map<String,Object> after,String reason,String key,String hash){
        return repo.insert("INSERT INTO hm_order_change(tenant_id,order_id,kind,status,old_price_cents,new_price_cents,difference_cents,before_data,after_data,reason,actor_id,actor_type,request_key,request_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",repo.tenant(),id,kind,status,cents(before,"price_cents"),cents(after,"price_cents"),cents(after,"price_cents")-cents(before,"price_cents"),encode(before),encode(after),reason,actor(),actorType(),key,hash);
    }
    public void recordReschedule(Map<String,Object> order,Map<String,Object> before,Map<String,Object> after,String reason){
        long id=record(number(order,"id"),"RESCHEDULE","APPLIED",before,after,reason,UUID.randomUUID().toString(),"");repo.jdbc().update("UPDATE hm_order_change SET applied_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",repo.tenant(),id);
    }
    private void apply(Map<String,Object> order,long change,Map<String,Object> after){
        validateCoverage(order,after);int delta=cents(after,"price_cents")-cents(order,"price_cents");
        repo.jdbc().update("UPDATE hm_order SET price_cents=?,area_fee_cents=?,contact_name=?,phone=?,address=?,district_code=?,pending_change_id=NULL,version=version+1 WHERE tenant_id=? AND id=?",after.get("price_cents"),after.get("area_fee_cents"),after.get("contact_name"),after.get("phone"),after.get("address"),after.get("district_code"),repo.tenant(),order.get("id"));
        if(delta!=0)repo.insert("INSERT INTO hm_order_item(tenant_id,order_id,name,price_cents,quantity,total_cents) VALUES(?,?,?,?,1,?)",repo.tenant(),order.get("id"),"订单变更 #"+change,delta,delta);
        repo.jdbc().update("UPDATE hm_order_change SET status='APPLIED',applied_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",repo.tenant(),change);
    }
    public Map<String,Object> pending(Map<String,Object> order){return order.get("pending_change_id")==null?null:repo.require("hm_order_change",number(order,"pending_change_id"),true);}
    public void settle(long orderId,long changeId){
        var order=repo.require("hm_order",orderId,true);var change=pending(order);
        check(change!=null&&number(change,"id")==changeId,"订单变更已更新，请刷新核对");
        check(cents(order,"paid_cents")-cents(order,"refunded_cents")==cents(change,"new_price_cents"),"变更差额尚未结清");
        apply(order,changeId,decode(change.get("after_data")));log(orderId,"CHANGE_APPLIED","变更单 #"+changeId+" 差额已结清");
    }
    @Transactional public void cancel(long orderId,long changeId,String reason,boolean admin,boolean canPrice){
        var order=owned(orderId,admin,true);var change=repo.require("hm_order_change",changeId,true);check(number(change,"order_id")==orderId,"变更单不属于当前订单");
        if(admin&&cents(change,"difference_cents")!=0)AdminScope.denyUnless(canPrice);
        if(!admin)AdminScope.denyUnless(number(change,"actor_id")==actor()&&number(change,"actor_type")==1);
        if("CANCELLED".equals(change.get("status")))return;
        check(Objects.equals(order.get("pending_change_id"),changeId)&&Set.of("PENDING_PAYMENT","PENDING_REFUND").contains(change.get("status")),"变更已生效或不在处理中，不能撤销");
        aftersales.cancelGenerated(changeId,"变更已撤销："+reason);
        repo.jdbc().update("UPDATE hm_order_change SET status='CANCELLED',cancelled_by=?,cancelled_type=?,cancel_reason=?,cancelled_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",actor(),actorType(),reason,repo.tenant(),changeId);
        repo.jdbc().update("UPDATE hm_order SET pending_change_id=NULL,version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),orderId);log(orderId,"CHANGE_CANCELLED","变更单 #"+changeId+"："+reason);
    }
    public List<Map<String,Object>> history(long id){var rows=repo.jdbc().queryForList("SELECT * FROM hm_order_change WHERE tenant_id=? AND order_id=? ORDER BY id DESC",repo.tenant(),id);for(var row:rows){row.put("before",decode(row.remove("before_data")));row.put("after",decode(row.remove("after_data")));row.remove("request_hash");row.remove("request_key");}return rows;}
    private void log(long id,String action,String detail){repo.insert("INSERT INTO hm_order_log(tenant_id,order_id,action,actor_id,detail) VALUES(?,?,?,?,?)",repo.tenant(),id,action,actor(),detail);}
}
