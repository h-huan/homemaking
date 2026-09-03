package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.pay.api.order.PayOrderApi;
import com.hm.module.pay.api.order.dto.PayOrderCreateReqDTO;
import com.hm.module.pay.api.refund.PayRefundApi;
import com.hm.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import com.hm.module.pay.enums.order.PayOrderStatusEnum;
import com.hm.module.pay.enums.refund.PayRefundStatusEnum;
import com.hm.module.pay.service.order.PayOrderService;
import com.hm.framework.tenant.core.util.TenantUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmPaymentService")
public class PaymentService {
    private final HmRepository repo;private final CustomerAccess customers;private final OrderService orders;
    private final PayOrderApi pay;private final PayRefundApi refunds;private final PayOrderService payOrderService;private final NotificationService notifications;private final SettlementService settlements;
    public PaymentService(HmRepository repo,CustomerAccess customers,OrderService orders,PayOrderApi pay,PayRefundApi refunds,PayOrderService payOrderService,NotificationService notifications,SettlementService settlements){this.repo=repo;this.customers=customers;this.orders=orders;this.pay=pay;this.refunds=refunds;this.payOrderService=payOrderService;this.notifications=notifications;this.settlements=settlements;}
    private String appKey(){String key=repo.jdbc().queryForObject("SELECT pay_app_key FROM hm_tenant_profile WHERE tenant_id=?",String.class,repo.tenant());check(key!=null&&!key.isBlank(),"当前租户尚未配置支付");return key;}
    @Transactional
    public long create(long id,String ip){var order=customers.own("hm_order",id,true);check("UNPAID".equals(order.get("status")),"订单不在待付款状态");
        if(order.get("pay_order_id")!=null)return number(order,"pay_order_id");
        var request=new PayOrderCreateReqDTO();request.setAppKey(appKey());request.setUserIp(ip);request.setUserId(customers.current());request.setUserType(1);
        LocalDateTime expiry=OrderService.time(order.get("created_at")).plusMinutes(30);check(expiry.isAfter(LocalDateTime.now()),"订单已超时，请重新预约");
        request.setMerchantOrderId("HM-"+repo.tenant()+"-"+id);String name=(String)order.get("service_name");request.setSubject(name.substring(0,Math.min(32,name.length())));request.setPrice(cents(order,"price_cents"));request.setExpireTime(expiry);
        long payId=pay.createOrder(request);repo.jdbc().update("UPDATE hm_order SET pay_order_id=?,version=version+1 WHERE tenant_id=? AND id=?",payId,repo.tenant(),id);return payId;
    }
    @Transactional
    public void syncOwned(long id){var order=customers.own("hm_order",id,false);if(order.get("pay_order_id")!=null)payOrderService.syncOrderQuietly(number(order,"pay_order_id"));sync(id);}
    /** The provider signature is verified by the pay module. Business state is derived only from its persisted result. */
    @Transactional
    public void sync(long id){var order=repo.require("hm_order",id,true);if(order.get("pay_order_id")==null)return;
        var paid=payOrderService.getOrder(number(order,"pay_order_id"));
        check(paid!=null&&Objects.equals(paid.getUserId(),number(order,"customer_id"))
                &&Objects.equals(paid.getUserType(),1)&&Objects.equals(paid.getMerchantOrderId(),"HM-"+repo.tenant()+"-"+id)&&paid.getPrice()==cents(order,"price_cents"),"支付记录与订单不匹配");
        if(!PayOrderStatusEnum.isSuccess(paid.getStatus()))return;
        if(cents(order,"paid_cents")>0)return;
        repo.jdbc().update("UPDATE hm_order SET paid_cents=price_cents,paid_at=?,version=version+1 WHERE tenant_id=? AND id=?",paid.getSuccessTime(),repo.tenant(),id);
        if("UNPAID".equals(order.get("status"))){repo.jdbc().update("UPDATE hm_order SET status='PAID' WHERE tenant_id=? AND id=?",repo.tenant(),id);orders.log(id,"PAID","");}
        else if("CANCELLED".equals(order.get("status"))){
            repo.insert("INSERT INTO hm_aftersale(tenant_id,customer_id,order_id,amount_cents,reason) VALUES(?,?,?,?,?)",repo.tenant(),order.get("customer_id"),id,order.get("price_cents"),"取消后到账，请退款");
            orders.log(id,"LATE_PAYMENT","等待退款处理");notifications.enqueue(number(order,"customer_id"),id,"PAYMENT_EXCEPTION","URGENT","late-pay:"+id,Map.of("orderId",id));
        }
    }
    /** Callback payload chooses a payment record, never a tenant. Tenant comes from the persisted payment record. */
    public void callback(long payId){var payments=repo.jdbc().queryForList("SELECT tenant_id FROM pay_order WHERE id=? AND deleted=FALSE",payId);if(payments.isEmpty())return;
        TenantUtils.execute(number(payments.get(0),"tenant_id"),()->{var rows=repo.jdbc().queryForList("SELECT id FROM hm_order WHERE tenant_id=? AND pay_order_id=?",repo.tenant(),payId);if(!rows.isEmpty())transactionalSelf().sync(number(rows.get(0),"id"));});
    }
    // Callback must enter the transactional proxy after tenant scope has been established.
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationContext context;
    private PaymentService transactionalSelf(){return context.getBean(PaymentService.class);}
    public com.hm.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO miniPay(long id,String appId,String ip){
        context.getBean(WechatGateway.class).app(appId,"MINI");
        var identities=repo.jdbc().queryForList("SELECT open_id FROM hm_wechat_identity WHERE app_id=? AND customer_id=?",appId,customers.current());
        check(identities.size()==1,"请先通过当前小程序登录");
        long payId=transactionalSelf().create(id,ip);
        var order=payOrderService.getOrder(payId);
        check(order!=null&&Objects.equals(order.getUserId(),customers.current())&&Objects.equals(order.getUserType(),1),"支付订单不属于当前客户");
        var channel=context.getBean(com.hm.module.pay.service.channel.PayChannelService.class).validPayChannel(order.getAppId(),"wx_lite");
        check(channel.getConfig() instanceof com.hm.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig,"支付渠道配置无效");
        var config=(com.hm.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig)channel.getConfig();
        check(appId.equals(config.getAppId()),"支付渠道与当前小程序不匹配");
        var request=new com.hm.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO();request.setId(payId);request.setChannelCode("wx_lite");
        request.setChannelExtras(Map.of("openid",(String)identities.get(0).get("open_id")));
        return payOrderService.submitOrder(request,ip);
    }
    @Transactional
    public void approveRefund(long aftersaleId,String ip){var a=repo.require("hm_aftersale",aftersaleId,true);var order=repo.require("hm_order",number(a,"order_id"),true);
        if(a.get("pay_refund_id")!=null)return;check(order.get("pay_order_id")!=null,"历史支付需先人工对账，不可自动退款");check("REQUESTED".equals(a.get("status")),"售后单不在待审核状态");
        check(cents(a,"amount_cents")<=cents(order,"paid_cents")-cents(order,"refunded_cents"),"退款金额超出可退款金额");
        var r=new PayRefundCreateReqDTO();r.setAppKey(appKey());r.setUserIp(ip);r.setUserId(number(order,"customer_id"));r.setUserType(1);r.setMerchantOrderId("HM-"+repo.tenant()+"-"+order.get("id"));r.setMerchantRefundId("HM-R-"+repo.tenant()+"-"+aftersaleId);String reason=(String)a.get("reason");r.setReason(reason.substring(0,Math.min(128,reason.length())));r.setPrice(cents(a,"amount_cents"));
        long refund=refunds.createRefund(r);repo.jdbc().update("UPDATE hm_aftersale SET status='REFUNDING',pay_refund_id=?,previous_order_status=? WHERE tenant_id=? AND id=?",refund,order.get("status"),repo.tenant(),aftersaleId);
        repo.jdbc().update("UPDATE hm_order SET status='REFUNDING',version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),order.get("id"));
    }
    @Transactional
    public void rejectRefund(long aftersaleId,String remark){
        var aftersale=repo.require("hm_aftersale",aftersaleId,true);
        check("REQUESTED".equals(aftersale.get("status")),"售后单不在待审核状态");
        String reason=CatalogService.s(remark);check(!reason.isBlank()&&reason.length()<=1000,"请填写 1000 字以内的驳回说明");
        repo.jdbc().update("UPDATE hm_aftersale SET status='REJECTED',audit_remark=? WHERE tenant_id=? AND id=?",reason,repo.tenant(),aftersaleId);
        orders.log(number(aftersale,"order_id"),"AFTERSALE_REJECTED",reason);
    }
    @Transactional
    public void syncRefund(long aftersaleId){var a=repo.require("hm_aftersale",aftersaleId,true);if(a.get("pay_refund_id")==null||"REFUNDED".equals(a.get("status")))return;var r=refunds.getRefund(number(a,"pay_refund_id"));
        if(r==null)return;
        check(r.getRefundPrice()!=null&&r.getRefundPrice()>0&&r.getRefundPrice()==cents(a,"amount_cents")&&Objects.equals(r.getMerchantRefundId(),"HM-R-"+repo.tenant()+"-"+aftersaleId)&&Objects.equals(r.getMerchantOrderId(),"HM-"+repo.tenant()+"-"+a.get("order_id")),"退款记录不匹配");
        if(PayRefundStatusEnum.isFailure(r.getStatus())){repo.jdbc().update("UPDATE hm_aftersale SET status='FAILED' WHERE tenant_id=? AND id=? AND status='REFUNDING'",repo.tenant(),aftersaleId);repo.jdbc().update("UPDATE hm_order SET status=?,version=version+1 WHERE tenant_id=? AND id=? AND status='REFUNDING'",Objects.toString(a.get("previous_order_status"),"PAID"),repo.tenant(),a.get("order_id"));return;}
        if(!PayRefundStatusEnum.isSuccess(r.getStatus()))return;
        var order=repo.require("hm_order",number(a,"order_id"),true);int total=Math.addExact(cents(order,"refunded_cents"),r.getRefundPrice());check(total<=cents(order,"paid_cents"),"累计退款超过支付金额");
        String next=total==cents(order,"paid_cents")?"REFUNDED":Objects.toString(a.get("previous_order_status"),"PAID");
        repo.jdbc().update("UPDATE hm_order SET refunded_cents=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",total,next,repo.tenant(),order.get("id"));repo.jdbc().update("UPDATE hm_aftersale SET status='REFUNDED' WHERE tenant_id=? AND id=?",repo.tenant(),aftersaleId);
        if(next.equals("REFUNDED"))orders.release(order,"CANCELLED");
        repo.jdbc().update("UPDATE hm_settlement SET refund_cents=?,net_cents=gross_cents-?,version=version+1 WHERE tenant_id=? AND order_id=? AND status='PENDING'",total,total,repo.tenant(),order.get("id"));
        settlements.refund(order,aftersaleId,r.getRefundPrice());
        orders.log(number(order,"id"),"REFUNDED","cents="+r.getRefundPrice());notifications.enqueue(number(order,"customer_id"),number(order,"id"),"REFUND_RESULT","IMPORTANT","refund:"+aftersaleId,Map.of("orderId",order.get("id")));
    }
    public void refundCallback(long refundId){var records=repo.jdbc().queryForList("SELECT tenant_id FROM pay_refund WHERE id=? AND deleted=FALSE",refundId);if(records.isEmpty())return;TenantUtils.execute(number(records.get(0),"tenant_id"),()->{var rows=repo.jdbc().queryForList("SELECT id FROM hm_aftersale WHERE tenant_id=? AND pay_refund_id=?",repo.tenant(),refundId);if(!rows.isEmpty())transactionalSelf().syncRefund(number(rows.get(0),"id"));});}
}
