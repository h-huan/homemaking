package com.hm.module.homemaking.service;

import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.AdminScope;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hm.module.homemaking.controller.BusinessTimeDeserializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

/** Append-only cash journal. Order balances are changed in the same transaction under the order lock. */
@Service
public class PaymentLedgerService {
    @org.springframework.beans.factory.annotation.Autowired private AftersaleService aftersales;
    public record Receipt(@NotNull @Pattern(regexp="CASH|WECHAT_TRANSFER|ALIPAY_TRANSFER|BANK_TRANSFER|OTHER") String channel,
            @Min(1) @Max(100000000) int amountCents, @NotNull @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime occurredAt,
            @NotBlank @Size(max=1000) String note, @NotNull @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String requestKey) {}
    public record Reversal(@Min(1) long receiptId,@NotNull @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime occurredAt,
            @NotBlank @Size(max=1000) String note,@NotNull @Pattern(regexp="[A-Za-z0-9_-]{8,80}") String requestKey) {}
    private final HmRepository repo; private final OrderService orders; private final SettlementService settlements; private final PaymentPolicyService policy;
    @org.springframework.beans.factory.annotation.Autowired private NotificationService notifications;
    @org.springframework.beans.factory.annotation.Autowired private OrderChangeService changes;
    public PaymentLedgerService(HmRepository repo,OrderService orders,SettlementService settlements,PaymentPolicyService policy){this.repo=repo;this.orders=orders;this.settlements=settlements;this.policy=policy;}
    private long operator(){var user=SecurityFrameworkUtils.getLoginUser();AdminScope.denyUnless(user!=null&&Objects.equals(user.getUserType(),2)&&AdminScope.current()!=null);return user.getId();}
    private void time(LocalDateTime at,LocalDateTime earliest){check(at!=null&&!at.isBefore(earliest)&&!at.isAfter(LocalDateTime.now().plusMinutes(5)),"实际发生时间须在订单创建/收款后且不能晚于当前时间");}
    private Long repeated(long order,Long aftersale,Long reversal,String kind,int amount,String channel,LocalDateTime time,String note,String key){
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_payment_entry WHERE tenant_id=? AND request_key=?",repo.tenant(),"manual:"+key);
        if(rows.isEmpty())return null;var e=rows.get(0);
        check(number(e,"order_id")==order&&Objects.equals(e.get("aftersale_id"),aftersale)&&Objects.equals(e.get("reversal_of"),reversal)&&kind.equals(e.get("kind"))&&cents(e,"amount_cents")==amount&&channel.equals(e.get("channel"))&&OrderService.time(e.get("occurred_at")).equals(time.withNano(0))&&note.equals(e.get("note")),"请求编号已用于其他收支记录，请刷新核对");
        return number(e,"id");
    }
    private long entry(long order,Long aftersale,Long reversal,String kind,String method,String channel,int amount,LocalDateTime occurred,Long operator,String note,String key){
        return repo.insert("INSERT INTO hm_payment_entry(tenant_id,order_id,aftersale_id,reversal_of,kind,payment_method,channel,amount_cents,occurred_at,operator_id,note,request_key) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",repo.tenant(),order,aftersale,reversal,kind,method,channel,amount,occurred.withNano(0),operator,note,key);
    }
    @Transactional public long receive(long id,Receipt r){
        long actor=operator();var order=repo.require("hm_order",id,true);
        Long previous=repeated(id,null,null,"RECEIPT",r.amountCents(),r.channel(),r.occurredAt(),r.note(),r.requestKey());if(previous!=null)return previous;
        var change=changes.pending(order);
        if(change!=null){
            check("PENDING_PAYMENT".equals(change.get("status"))&&r.amountCents()==cents(change,"difference_cents"),"请按待补差额确认收款");
            check(order.get("pay_order_id")==null&&"OFFLINE".equals(order.get("payment_method")),"变更补差额仅适用于已登记线下收款的订单");
            check(policy.options().offlineAvailable(),"当前未开放线下收款");time(r.occurredAt(),OrderService.time(change.get("created_at")).withNano(0));
            long receipt=entry(id,null,null,"RECEIPT","OFFLINE",r.channel(),r.amountCents(),r.occurredAt(),actor,r.note(),"manual:"+r.requestKey());
            repo.jdbc().update("UPDATE hm_payment_entry SET order_change_id=? WHERE tenant_id=? AND id=?",change.get("id"),repo.tenant(),receipt);
            repo.jdbc().update("UPDATE hm_order SET paid_cents=?,version=version+1 WHERE tenant_id=? AND id=?",Math.addExact(cents(order,"paid_cents"),r.amountCents()),repo.tenant(),id);
            changes.settle(id,number(change,"id"));orders.log(id,"OFFLINE_RECEIVED","变更补款流水="+receipt);return receipt;
        }
        check(policy.options().offlineAvailable(),"该租户当前仅开放在线支付");
        check("UNPAID".equals(order.get("status"))&&cents(order,"paid_cents")==0,"当前订单不能重复确认收款");
        check(order.get("pay_order_id")==null,"已发起线上支付，请先核对线上结果；不能重复登记线下收款");
        check(r.amountCents()==cents(order,"price_cents"),"V1 线下收款须确认整单实际金额，不能按免费或部分付款处理");
        time(r.occurredAt(),OrderService.time(order.get("created_at")).withNano(0));
        var booking=repo.require("hm_booking",number(order,"booking_id"),false);check(OrderService.time(booking.get("ends_at")).isAfter(LocalDateTime.now()),"预约时段已结束，请调整预约后再收款");
        long receipt=entry(id,null,null,"RECEIPT","OFFLINE",r.channel(),r.amountCents(),r.occurredAt(),actor,r.note(),"manual:"+r.requestKey());
        repo.jdbc().update("UPDATE hm_order SET payment_method='OFFLINE',payment_expires_at=NULL,paid_cents=?,paid_at=?,status='PAID',version=version+1 WHERE tenant_id=? AND id=?",r.amountCents(),r.occurredAt().withNano(0),repo.tenant(),id);
        orders.log(id,"OFFLINE_RECEIVED","流水="+receipt+"，渠道="+r.channel()+"，金额（分）="+r.amountCents());return receipt;
    }
    @Transactional public long refund(long aftersale,Receipt r){
        long actor=operator();long id=number(repo.require("hm_aftersale",aftersale,false),"order_id");var order=repo.require("hm_order",id,true);var a=repo.require("hm_aftersale",aftersale,true);
        Long previous=repeated(id,aftersale,null,"REFUND",-r.amountCents(),r.channel(),r.occurredAt(),r.note(),r.requestKey());if(previous!=null)return previous;
        check("OFFLINE".equals(order.get("payment_method"))&&order.get("pay_order_id")==null,"仅支持已登记的线下收款退款；线上订单须原路退款");
        check(Set.of("PARTIAL_REFUND","FULL_REFUND").contains(a.get("type"))&&"REQUESTED".equals(a.get("status"))&&r.amountCents()==cents(a,"amount_cents"),"请按待审核退款售后单的申请金额登记实际退款");
        check(r.amountCents()<=cents(order,"paid_cents")-cents(order,"refunded_cents"),"退款金额超过剩余实收款");
        check(hasReceipt(id),"缺少线下收款凭证，请先对账");time(r.occurredAt(),OrderService.time(order.get("paid_at")));
        if(a.get("order_change_id")!=null){var change=changes.pending(order);check(change!=null&&number(change,"id")==number(a,"order_change_id")&&"PENDING_REFUND".equals(change.get("status")),"退差额变更单已失效");}
        long receipt=entry(id,aftersale,null,"REFUND","OFFLINE",r.channel(),-r.amountCents(),r.occurredAt(),actor,r.note(),"manual:"+r.requestKey());
        int total=cents(order,"refunded_cents")+r.amountCents();String next=total==cents(order,"paid_cents")?"REFUNDED":order.get("status").toString();
        aftersales.refundCompleted(aftersale);repo.jdbc().update("UPDATE hm_order SET refunded_cents=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",total,next,repo.tenant(),id);
        repo.jdbc().update("UPDATE hm_aftersale SET status='REFUNDED',previous_order_status=?,audit_remark=? WHERE tenant_id=? AND id=?",order.get("status"),r.note(),repo.tenant(),aftersale);
        if(a.get("order_change_id")!=null){repo.jdbc().update("UPDATE hm_payment_entry SET order_change_id=? WHERE tenant_id=? AND id=?",a.get("order_change_id"),repo.tenant(),receipt);changes.settle(id,number(a,"order_change_id"));}
        if(next.equals("REFUNDED"))orders.release(order,"CANCELLED");
        repo.jdbc().update("UPDATE hm_settlement SET refund_cents=?,net_cents=gross_cents-?,version=version+1 WHERE tenant_id=? AND order_id=? AND status='PENDING'",total,total,repo.tenant(),id);
        settlements.refund(order,aftersale,r.amountCents());
        orders.log(id,"OFFLINE_REFUNDED","流水="+receipt+"，金额（分）="+r.amountCents());
        notifications.enqueue(number(order,"customer_id"),id,"REFUND_RESULT","IMPORTANT","refund:"+aftersale,Map.of("orderId",id));return receipt;
    }
    @Transactional public long reverse(long id,Reversal r){
        long actor=operator();var order=repo.require("hm_order",id,true);
        changes.requireSettled(order);
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_payment_entry WHERE tenant_id=? AND order_id=? AND id=? AND kind='RECEIPT' AND payment_method='OFFLINE'",repo.tenant(),id,r.receiptId());check(rows.size()==1,"线下收款凭证不存在");var original=rows.get(0);
        Long previous=repeated(id,null,r.receiptId(),"REVERSAL",-cents(original,"amount_cents"),original.get("channel").toString(),r.occurredAt(),r.note(),r.requestKey());if(previous!=null)return previous;
        check("OFFLINE".equals(order.get("payment_method"))&&Set.of("PAID","ASSIGNED").contains(order.get("status"))&&Set.of("WAITING","ACCEPTED").contains(order.get("fulfillment_status")),"仅未开始履约的误登记收款可冲正，其他情况请走售后退款");
        check(cents(order,"refunded_cents")==0&&cents(order,"paid_cents")==cents(original,"amount_cents"),"已退款或金额不一致，不能冲正");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_payment_entry WHERE tenant_id=? AND reversal_of=?",Long.class,repo.tenant(),r.receiptId())==0,"该收款已冲正");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_aftersale WHERE tenant_id=? AND order_id=? AND status IN ('REQUESTED','SCHEDULED','IN_PROGRESS','AWAITING_CONFIRMATION','REFUNDING')",Long.class,repo.tenant(),id)==0,"存在进行中的售后，请先处理售后");
        time(r.occurredAt(),OrderService.time(original.get("occurred_at")));
        long receipt=entry(id,null,r.receiptId(),"REVERSAL","OFFLINE",original.get("channel").toString(),-cents(original,"amount_cents"),r.occurredAt(),actor,r.note(),"manual:"+r.requestKey());
        repo.jdbc().update("UPDATE hm_order SET paid_cents=0,paid_at=NULL,status='UNPAID',fulfillment_status='WAITING',version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),id);
        orders.log(id,"RECEIPT_REVERSED","原流水="+r.receiptId()+"，冲正流水="+receipt);return receipt;
    }
    private boolean hasReceipt(long id){return repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_payment_entry e WHERE e.tenant_id=? AND e.order_id=? AND e.kind='RECEIPT' AND e.payment_method='OFFLINE' AND NOT EXISTS(SELECT 1 FROM hm_payment_entry r WHERE r.tenant_id=e.tenant_id AND r.reversal_of=e.id)",Long.class,repo.tenant(),id)>0;}
    public void onlineReceipt(Map<String,Object> order,LocalDateTime at){entry(number(order,"id"),null,null,"RECEIPT","ONLINE","WX_MINI",cents(order,"price_cents"),Objects.requireNonNullElseGet(at,LocalDateTime::now),null,"支付渠道确认","online:"+order.get("pay_order_id"));}
    public void onlineRefund(Map<String,Object> order,long aftersale,int amount,LocalDateTime at){entry(number(order,"id"),aftersale,null,"REFUND","ONLINE","WX_MINI",-amount,Objects.requireNonNullElseGet(at,LocalDateTime::now),null,"支付渠道退款确认","online-refund:"+aftersale);}
    public Object orderEntries(long id){repo.require("hm_order",id,false);return repo.jdbc().queryForList("SELECT e.*,u.nickname AS operator_name FROM hm_payment_entry e LEFT JOIN system_users u ON u.id=e.operator_id AND u.tenant_id=e.tenant_id WHERE e.tenant_id=? AND e.order_id=? ORDER BY e.id",repo.tenant(),id);}
    public Map<String,Object> report(LocalDate from,LocalDate to,int page,int size){
        check(from!=null&&to!=null&&!to.isBefore(from)&&!to.isAfter(from.plusDays(366)),"收支查询范围最多 366 天");size=Math.min(Math.max(size,1),100);page=Math.max(page,1);
        String where=" WHERE e.tenant_id=? AND e.occurred_at>=? AND e.occurred_at<?"+repo.scope("hm_payment_entry","e");Object[] args={repo.tenant(),from.atStartOfDay(),to.plusDays(1).atStartOfDay()};
        var summary=repo.jdbc().queryForMap("SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN e.kind='RECEIPT' THEN e.amount_cents ELSE 0 END),0) AS received_cents,COALESCE(SUM(CASE WHEN e.kind='REFUND' THEN -e.amount_cents ELSE 0 END),0) AS refunded_cents,COALESCE(SUM(CASE WHEN e.kind='REVERSAL' THEN -e.amount_cents ELSE 0 END),0) AS reversed_cents,COALESCE(SUM(e.amount_cents),0) AS net_cents FROM hm_payment_entry e"+where,args);
        var paged=new ArrayList<>(Arrays.asList(args));paged.add(size);paged.add((page-1)*size);
        return Map.of("summary",summary,"list",repo.jdbc().queryForList("SELECT e.*,u.nickname AS operator_name FROM hm_payment_entry e LEFT JOIN system_users u ON u.id=e.operator_id AND u.tenant_id=e.tenant_id"+where+" ORDER BY e.id DESC LIMIT ? OFFSET ?",paged.toArray()));
    }
}
