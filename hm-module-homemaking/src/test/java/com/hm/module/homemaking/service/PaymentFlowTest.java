package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.AdminScope;
import com.hm.module.pay.api.order.PayOrderApi;
import com.hm.module.pay.api.refund.PayRefundApi;
import com.hm.module.pay.api.refund.dto.PayRefundRespDTO;
import com.hm.module.pay.dal.dataobject.app.PayAppDO;
import com.hm.module.pay.dal.dataobject.channel.PayChannelDO;
import com.hm.module.pay.dal.dataobject.order.PayOrderDO;
import com.hm.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig;
import com.hm.module.pay.service.app.PayAppService;
import com.hm.module.pay.service.channel.PayChannelService;
import com.hm.module.pay.service.order.PayOrderService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(BusinessIsolationTest.Config.class)
class PaymentFlowTest {
    @Autowired JdbcTemplate jdbc;@Autowired DataSource dataSource;@Autowired OrderService orders;@Autowired PaymentService payments;
    @Autowired PaymentLedgerService ledger;@Autowired PaymentPolicyService policy;@Autowired HmRepository repo;
    @Autowired OrderChangeService changes;
    @Autowired WorkerService workerService;
    @Autowired PayAppService apps;@Autowired PayChannelService channels;@Autowired PayOrderApi payApi;@Autowired PayOrderService payOrders;@Autowired PayRefundApi refunds;
    long id;
    @BeforeEach void seed(){
        jdbc.execute("DROP ALL OBJECTS");reset(apps,channels,payApi,payOrders,refunds);
        var scripts=new ResourceDatabasePopulator(new FileSystemResource("../sql/mysql/hm-homemaking.sql"),new FileSystemResource("../sql/mysql/upgrades/V002__operations_and_portal.sql"));scripts.setSqlScriptEncoding("UTF-8");scripts.execute(dataSource);BusinessTestSchema.payment(dataSource);
        jdbc.execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,nickname VARCHAR(40))");jdbc.update("INSERT INTO system_users VALUES(42,1,'Finance')");
        jdbc.execute("CREATE TABLE pay_app(id BIGINT,tenant_id BIGINT,app_key VARCHAR(40),deleted BOOLEAN DEFAULT FALSE)");jdbc.update("INSERT INTO pay_app VALUES(100,1,'local-test',FALSE)");
        jdbc.execute("CREATE TABLE pay_order(id BIGINT,tenant_id BIGINT,deleted BOOLEAN DEFAULT FALSE)");jdbc.execute("CREATE TABLE pay_refund(id BIGINT,tenant_id BIGINT,deleted BOOLEAN DEFAULT FALSE)");
        jdbc.update("INSERT INTO hm_customer(id,nickname) VALUES(1,'Customer')");jdbc.update("INSERT INTO hm_customer_tenant(tenant_id,customer_id) VALUES(1,1)");
        jdbc.update("INSERT INTO hm_store(id,tenant_id,name) VALUES(1,1,'Store'),(2,1,'Other')");
        jdbc.update("INSERT INTO hm_worker(id,tenant_id,store_id,name) VALUES(1,1,1,'Worker')");
        jdbc.update("INSERT INTO hm_service(id,tenant_id,store_id,name,description,price_cents,duration_minutes) VALUES(1,1,1,'Clean','',10000,60)");
        jdbc.update("INSERT INTO hm_customer_address(id,tenant_id,customer_id,contact_name,phone,address) VALUES(1,1,1,'C','13800000001','Address')");
        jdbc.update("INSERT INTO hm_worker_skill VALUES(1,1,1)");jdbc.update("INSERT INTO hm_worker_area VALUES(1,1,'*')");
        var start=LocalDate.now().plusDays(2).atTime(9,0);jdbc.update("INSERT INTO hm_worker_schedule(tenant_id,worker_id,starts_at,ends_at) VALUES(1,1,?,?)",start.minusHours(1),start.plusHours(8));
        customer();id=orders.book(new OrderService.Book(1L,1L,start,1L,"payment-order"));admin();
    }
    @AfterEach void clear(){AdminScope.set(null);TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    void login(long user,int type){TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);var u=new LoginUser();u.setId(user);u.setTenantId(1L);u.setUserType(type);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
    void customer(){AdminScope.set(null);login(1,1);}
    void admin(){login(42,2);AdminScope.set(new AdminScope(1,false,"TENANT",Set.of(),null));}
    LocalDateTime now(){return LocalDateTime.now().withNano(0);}
    PaymentLedgerService.Receipt receipt(String key,int amount){return new PaymentLedgerService.Receipt("CASH",amount,now(),"Actual cash counted",key);}
    Map<String,Object> order(){return repo.require("hm_order",id,false);}
    Map<String,Object> summary(){return (Map<String,Object>)ledger.report(LocalDate.now(),LocalDate.now(),1,20).get("summary");}
    long aftersale(int amount){customer();long a=orders.aftersale(new OrderService.Aftersale(id,amount,"Refund requested"));admin();return a;}
    void ready(String mode){
        jdbc.update("UPDATE hm_tenant_profile SET payment_mode=?,mini_app_id='wxTestOnly123456',pay_app_key='local-test' WHERE tenant_id=1",mode);
        jdbc.update("INSERT INTO hm_wechat_app(tenant_id,app_id,kind,platform_id,secret_env,enabled) VALUES(1,'wxTestOnly123456','MINI','test','HM_WECHAT_MINI_TEST_SECRET',TRUE)");
        var app=new PayAppDO();app.setId(100L);when(apps.validPayApp("local-test")).thenReturn(app);
        var config=new WxPayClientConfig();config.setAppId("wxTestOnly123456");config.setMchId("TEST_ONLY_MERCHANT");config.setApiVersion("v2");config.setMchKey("TEST_ONLY_PLACEHOLDER");config.setKeyContent("TEST_ONLY_PLACEHOLDER");
        var channel=new PayChannelDO();channel.setId(101L);channel.setAppId(100L);channel.setTenantId(1L);channel.setConfig(config);when(channels.validPayChannel(100L,"wx_lite")).thenReturn(channel);
    }
    PayOrderDO online(){
        ready("BOTH");customer();when(payApi.createOrder(any())).thenReturn(500L);assertEquals(500L,payments.create(id,"127.0.0.1"));
        var paid=new PayOrderDO();paid.setId(500L);paid.setUserId(1L);paid.setUserType(1);paid.setMerchantOrderId("HM-1-"+id);paid.setPrice(10000);paid.setStatus(10);paid.setSuccessTime(now());when(payOrders.getOrder(500L)).thenReturn(paid);
        jdbc.update("INSERT INTO pay_order VALUES(500,1,FALSE)");return paid;
    }
    void complete(){
        jdbc.update("UPDATE hm_order SET status='IN_SERVICE',fulfillment_status='STARTED' WHERE id=?",id);
        jdbc.update("INSERT INTO hm_fulfillment_evidence(tenant_id,order_id,worker_id,storage_key,content_type,phase,note) VALUES(1,?,1,'test-only-photo','image/jpeg','AFTER','fixture')",id);orders.complete(id);
    }
    @Test void defaultOfflineKeepsPriceAndDoesNotExpireAtThirtyMinutes(){
        assertFalse(policy.options().onlineAvailable());assertTrue(policy.options().offlineAvailable());assertEquals(10000,HmRepository.cents(order(),"price_cents"));assertEquals(0,HmRepository.cents(order(),"paid_cents"));
        jdbc.update("UPDATE hm_order SET created_at=? WHERE id=?",now().minusHours(2),id);orders.expire(id);assertEquals("UNPAID",order().get("status"));customer();assertThrows(Exception.class,()->payments.create(id,"local"));verifyNoInteractions(payApi);
    }
    @ParameterizedTest @ValueSource(strings={"OFFLINE","ONLINE","BOTH"})
    void missingMerchantNeverExposesOnlineEntrance(String mode){jdbc.update("UPDATE hm_tenant_profile SET payment_mode=? WHERE tenant_id=1",mode);assertFalse(policy.options().onlineAvailable());assertTrue(policy.options().offlineAvailable());}
    @Test void validOnlineOnlyModeRejectsOfflineReceipt(){ready("ONLINE");assertTrue(policy.options().onlineAvailable());assertFalse(policy.options().offlineAvailable());assertThrows(Exception.class,()->ledger.receive(id,receipt("blocked-online",10000)));}
    @Test void mismatchedOrDisabledMerchantFallsBackToOffline(){ready("BOTH");jdbc.update("UPDATE pay_app SET tenant_id=2 WHERE id=100");assertFalse(policy.options().onlineAvailable());jdbc.update("UPDATE pay_app SET tenant_id=1 WHERE id=100");when(channels.validPayChannel(100L,"wx_lite")).thenThrow(new IllegalArgumentException("disabled"));assertFalse(policy.options().onlineAvailable());assertTrue(policy.options().offlineAvailable());}
    @ParameterizedTest @ValueSource(strings={"CASH","WECHAT_TRANSFER","ALIPAY_TRANSFER","BANK_TRANSFER","OTHER"})
    void eachOfflineChannelRecordsActualReceiptAndActor(String channel){
        var r=new PaymentLedgerService.Receipt(channel,10000,now(),"Receipt checked","channel-"+channel);long entry=ledger.receive(id,r);
        assertEquals("PAID",order().get("status"));assertEquals(10000,HmRepository.cents(order(),"paid_cents"));
        var row=jdbc.queryForMap("SELECT * FROM hm_payment_entry WHERE id=?",entry);assertEquals(42L,row.get("operator_id"));assertEquals(channel,row.get("channel"));assertEquals(r.note(),row.get("note"));assertEquals(10000,HmRepository.cents(summary(),"net_cents"));
    }
    @ParameterizedTest @ValueSource(ints={-1,0,9999,10001})
    void receiptMustMatchEntireServerPricedOrder(int amount){assertThrows(Exception.class,()->ledger.receive(id,receipt("wrong-amount",amount)));assertEquals(0,HmRepository.cents(order(),"paid_cents"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));}
    @Test void receiptRetriesAreIdempotentButChangedPayloadAndSecondReceiptAreDenied(){var r=receipt("same-request",10000);long entry=ledger.receive(id,r);assertEquals(entry,ledger.receive(id,r));assertThrows(Exception.class,()->ledger.receive(id,receipt("same-request",9999)));assertThrows(Exception.class,()->ledger.receive(id,receipt("second-request",10000)));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));}
    @Test void onlyOneConcurrentReceiptCommits()throws Exception{
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var calls=new ArrayList<Future<Boolean>>();for(int n=0;n<2;n++){String key="parallel-"+n;calls.add(pool.submit(()->{admin();try{start.await();ledger.receive(id,receipt(key,10000));return true;}catch(org.springframework.web.server.ResponseStatusException e){return false;}finally{clear();}}));}start.countDown();int successes=0;for(var call:calls)if(call.get(10,TimeUnit.SECONDS))successes++;assertEquals(1,successes);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));}finally{pool.shutdownNow();}
    }
    @Test void customerAndForeignStoreCannotConfirmReceipt(){customer();assertThrows(Exception.class,()->ledger.receive(id,receipt("customer-denied",10000)));admin();AdminScope.set(new AdminScope(1,false,"STORES",Set.of(2L),null));assertThrows(Exception.class,()->ledger.receive(id,receipt("store-denied",10000)));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));}
    @Test void initiatedOnlinePaymentCannotAlsoBeReceivedOffline(){online();admin();jdbc.update("UPDATE hm_tenant_profile SET payment_mode='OFFLINE' WHERE tenant_id=1");assertThrows(Exception.class,()->ledger.receive(id,receipt("online-inflight",10000)));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));}
    @Test void reversalKeepsOriginalEvidenceAndAllowsCorrectReceipt(){
        long received=ledger.receive(id,receipt("original-receipt",10000));var reverse=new PaymentLedgerService.Reversal(received,now(),"Mistaken registration","reverse-receipt");long reversal=ledger.reverse(id,reverse);assertEquals(reversal,ledger.reverse(id,reverse));assertEquals(0,HmRepository.cents(summary(),"net_cents"));assertEquals("UNPAID",order().get("status"));
        ledger.receive(id,receipt("correct-receipt",10000));assertThrows(Exception.class,()->ledger.reverse(id,new PaymentLedgerService.Reversal(received,now(),"Duplicate reversal","another-reversal")));assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));assertEquals(10000,HmRepository.cents(summary(),"net_cents"));
    }
    @Test void completedOrderUsesRefundInsteadOfReceiptReversal(){long entry=ledger.receive(id,receipt("paid-completed",10000));complete();assertThrows(Exception.class,()->ledger.reverse(id,new PaymentLedgerService.Reversal(entry,now(),"Wrong stage","reverse-completed")));}
    @Test void partialThenFullRefundUpdatesOrderSettlementAndJournal(){
        jdbc.update("INSERT INTO hm_commission_rule(tenant_id,platform_bps,worker_bps,cycle_days,enabled) VALUES(1,1000,2000,30,TRUE)");
        ledger.receive(id,receipt("settled-receipt",10000));complete();assertEquals(10000,jdbc.queryForObject("SELECT net_cents FROM hm_settlement WHERE order_id=?",Integer.class,id));
        long a=aftersale(2000);var r=receipt("partial-refund",2000);long entry=ledger.refund(a,r);assertEquals(entry,ledger.refund(a,r));assertEquals("COMPLETED",order().get("status"));assertEquals(8000,jdbc.queryForObject("SELECT net_cents FROM hm_settlement_allocation WHERE order_id=?",Integer.class,id));
        assertEquals(800,jdbc.queryForObject("SELECT platform_cents FROM hm_settlement_allocation WHERE order_id=?",Integer.class,id));
        long b=aftersale(8000);ledger.refund(b,receipt("remaining-refund",8000));assertEquals("REFUNDED",order().get("status"));assertEquals(0,jdbc.queryForObject("SELECT net_cents FROM hm_settlement WHERE order_id=?",Integer.class,id));assertEquals(0,HmRepository.cents(summary(),"net_cents"));assertEquals(0,jdbc.queryForObject("SELECT SUM(amount_cents) FROM hm_settlement_entry WHERE order_id=?",Integer.class,id));
    }
    @Test void refundsRemainPossibleAfterModeSwitchAndAmountsCannotBeForged(){ledger.receive(id,receipt("mode-receipt",10000));long a=aftersale(2000);ready("ONLINE");assertThrows(Exception.class,()->ledger.refund(a,receipt("over-refund",3000)));ledger.refund(a,receipt("mode-refund",2000));assertEquals(2000,HmRepository.cents(order(),"refunded_cents"));}
    @Test void manualRefundCannotPretendToRefundOnlinePayment(){online();payments.sync(id);admin();long a=aftersale(1000);assertThrows(Exception.class,()->ledger.refund(a,receipt("wrong-channel",1000)));}
    @Test void onlineCallbackUsesPersistedTenantAndStillWorksAfterDisablingOnline(){
        online();jdbc.update("UPDATE hm_tenant_profile SET payment_mode='OFFLINE' WHERE tenant_id=1");TenantContextHolder.setTenantId(2L);payments.callback(500);assertEquals(2L,TenantContextHolder.getTenantId());TenantContextHolder.setTenantId(1L);payments.callback(500);assertEquals("PAID",order().get("status"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));
    }
    @Test void forgedOnlineAmountIsRejectedWithoutAnyLedgerMutation(){var paid=online();paid.setPrice(1);assertThrows(Exception.class,()->payments.sync(id));assertEquals(0,HmRepository.cents(order(),"paid_cents"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));}
    @Test void onlineRefundCallbacksAreIdempotentAndPreserveOriginalRoute(){
        online();payments.sync(id);long a=aftersale(2000);when(refunds.createRefund(any())).thenReturn(600L);payments.approveRefund(a,"local");
        var refund=new PayRefundRespDTO();refund.setStatus(10);refund.setRefundPrice(2000);refund.setMerchantOrderId("HM-1-"+id);refund.setMerchantRefundId("HM-R-1-"+a);refund.setSuccessTime(now());when(refunds.getRefund(600L)).thenReturn(refund);jdbc.update("INSERT INTO pay_refund VALUES(600,1,FALSE)");
        AdminScope.set(null);jdbc.update("UPDATE hm_tenant_profile SET payment_mode='OFFLINE' WHERE tenant_id=1");TenantContextHolder.setTenantId(2L);payments.refundCallback(600);assertEquals(2L,TenantContextHolder.getTenantId());TenantContextHolder.setTenantId(1L);payments.refundCallback(600);assertEquals(2000,HmRepository.cents(order(),"refunded_cents"));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));
    }
    @Test void onlineDeadlineStillCancelsAndReleasesSlots(){online();jdbc.update("UPDATE hm_order SET payment_expires_at=? WHERE id=?",now().minusMinutes(1),id);orders.expire(id);assertEquals("CANCELLED",order().get("status"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Integer.class));}
    @Test void financeTotalsAndRowsAreStoreScoped(){ledger.receive(id,receipt("private-receipt",10000));AdminScope.set(new AdminScope(1,false,"STORES",Set.of(2L),null));assertEquals(0,HmRepository.number(summary(),"total"));assertEquals(0,HmRepository.cents(summary(),"net_cents"));assertTrue(((List<?>)ledger.report(LocalDate.now(),LocalDate.now(),1,20).get("list")).isEmpty());}

    @Nested class OrderChanges {
        OrderChangeService.Request price(int amount,String key){return new OrderChangeService.Request(HmRepository.number(order(),"version"),null,amount,"Customer agreed revised price",key);}
        OrderChangeService.Request address(String district){var r=new OrderChangeService.Request(HmRepository.number(order(),"version"),new OrderChangeService.Contact("New contact","13800000002","New address",district),null,"Customer changed address",UUID.randomUUID().toString());var preview=changes.preview(id,r,true,false);return new OrderChangeService.Request(r.version(),r.contact(),null,r.reason(),r.requestKey(),HmRepository.cents((Map<String,Object>)preview.get("after"),"price_cents"));}
        Map<String,Object> change(long change){return repo.require("hm_order_change",change,false);}
        long refundFor(long change){return jdbc.queryForObject("SELECT id FROM hm_aftersale WHERE order_change_id=?",Long.class,change);}
        @Test void unpaidPriceChangeIsVersionedAndKeepsItemTotalConsistent(){
            var request=price(12000,"unpaid-change");long change=changes.create(id,request,true,true);
            assertEquals(change,changes.create(id,request,true,true));assertEquals(12000,HmRepository.cents(order(),"price_cents"));assertEquals(0,HmRepository.cents(order(),"paid_cents"));
            assertEquals(12000,jdbc.queryForObject("SELECT SUM(total_cents) FROM hm_order_item WHERE order_id=?",Integer.class,id));
            assertEquals("APPLIED",change(change).get("status"));assertThrows(Exception.class,()->changes.create(id,new OrderChangeService.Request(request.version(),null,13000,request.reason(),request.requestKey()),true,true));
        }
        @Test void addressChangeKeepsBeforeAndAfterSnapshots(){
            long change=changes.create(id,address(""),true,false);var history=changes.history(id).get(0);
            assertEquals("Address",((Map<?,?>)history.get("before")).get("address"));assertEquals("New address",((Map<?,?>)history.get("after")).get("address"));assertEquals("APPLIED",change(change).get("status"));
        }
        @Test void manualPriceRequiresPricePermissionEvenWithAddressPermission(){assertThrows(org.springframework.security.access.AccessDeniedException.class,()->changes.create(id,price(1,"forged-price"),true,false));assertEquals(10000,HmRepository.cents(order(),"price_cents"));}
        @Test void paidIncreaseRetainsOriginalUntilExactSupplementAndBlocksOtherOperations(){
            ledger.receive(id,receipt("original-cash",10000));long change=changes.create(id,price(13000,"price-increase"),true,true);
            assertEquals(10000,HmRepository.cents(order(),"price_cents"));assertEquals("PENDING_PAYMENT",change(change).get("status"));
            assertThrows(Exception.class,()->orders.assign(id,1));assertThrows(Exception.class,()->orders.cancel(id,true));assertThrows(Exception.class,()->orders.start(id));
            customer();assertThrows(Exception.class,()->orders.aftersale(new OrderService.Aftersale(id,1000,"Refund")));admin();
            assertThrows(Exception.class,()->ledger.receive(id,receipt("wrong-difference",13000)));
            var payment=receipt("exact-difference",3000);long entry=ledger.receive(id,payment);assertEquals(entry,ledger.receive(id,payment));
            assertEquals(13000,HmRepository.cents(order(),"price_cents"));assertEquals(13000,HmRepository.cents(order(),"paid_cents"));assertNull(order().get("pending_change_id"));assertEquals("APPLIED",change(change).get("status"));assertDoesNotThrow(()->orders.assign(id,1));
        }
        @Test void reductionAndLaterIncreaseEnterNormalRefundAndSettlementTotals(){
            ledger.receive(id,receipt("start-paid",10000));long reduction=changes.create(id,price(8000,"reduce-price"),true,true);
            assertEquals(10000,HmRepository.cents(order(),"price_cents"));assertThrows(Exception.class,()->payments.rejectRefund(refundFor(reduction),"Cannot reject financial obligation"));
            ledger.refund(refundFor(reduction),receipt("refund-difference",2000));assertEquals(8000,HmRepository.cents(order(),"price_cents"));assertEquals("PAID",order().get("status"));
            changes.create(id,price(12000,"raise-price-again"),true,true);ledger.receive(id,receipt("receive-next-diff",4000));complete();
            assertEquals(12000,jdbc.queryForObject("SELECT net_cents FROM hm_settlement WHERE order_id=?",Integer.class,id));assertEquals(12000,HmRepository.cents(summary(),"net_cents"));
            long full=aftersale(12000);ledger.refund(full,receipt("refund-final-order",12000));assertEquals("REFUNDED",order().get("status"));assertEquals(0,HmRepository.cents(summary(),"net_cents"));
        }
        @Test void cancellingPendingReductionPreservesOriginalAndClosesRefundRequest(){
            ledger.receive(id,receipt("paid-cancel",10000));long change=changes.create(id,price(9000,"reduce-to-cancel"),true,true);long refund=refundFor(change);
            changes.cancel(id,change,"Customer retained original contract",true,true);changes.cancel(id,change,"Retry",true,true);
            assertNull(order().get("pending_change_id"));assertEquals(10000,HmRepository.cents(order(),"price_cents"));assertEquals("CANCELLED",change(change).get("status"));assertEquals("REJECTED",repo.require("hm_aftersale",refund,false).get("status"));
            assertThrows(Exception.class,()->ledger.refund(refund,receipt("cancelled-diff-refund",1000)));assertEquals(10000,HmRepository.cents(summary(),"net_cents"));
        }
        @Test void completedOrNormallyRefundedOrdersCannotBeRepriced(){
            ledger.receive(id,receipt("before-normal-refund",10000));long a=aftersale(1000);ledger.refund(a,receipt("normal-refund",1000));assertThrows(Exception.class,()->changes.create(id,price(11000,"after-refund-change"),true,true));complete();assertThrows(Exception.class,()->changes.create(id,address(""),true,false));
        }
        @Test void issuedOnlinePaymentPriceCannotBeOverwritten(){online();admin();assertThrows(Exception.class,()->changes.create(id,price(9000,"inflight-change"),true,true));payments.sync(id);assertThrows(Exception.class,()->changes.create(id,price(11000,"paid-online-change"),true,true));assertEquals(10000,HmRepository.cents(order(),"price_cents"));assertEquals(10000,HmRepository.cents(order(),"paid_cents"));}
        @Test void priceNeutralOnlineAddressChangeDoesNotBreakCallback(){online();admin();changes.create(id,address(""),true,false);payments.sync(id);assertEquals("PAID",order().get("status"));assertEquals("New address",order().get("address"));}
        @Test void areaFeeIsRecalculatedFromFrozenOriginalAndInvalidCoverageRollsBack(){
            jdbc.update("INSERT INTO hm_service_area(id,tenant_id,name,district_code,extra_cents) VALUES(1,1,'New district','B',2000)");jdbc.update("INSERT INTO hm_service_area_relation VALUES(1,1,1)");
            var preview=changes.preview(id,address("B"),true,false);assertEquals(2000,preview.get("differenceCents"));changes.create(id,address("B"),true,false);assertEquals(12000,HmRepository.cents(order(),"price_cents"));
            assertThrows(Exception.class,()->changes.create(id,address("UNKNOWN"),true,false));assertEquals("B",order().get("district_code"));
        }
        @Test void changedWorkerCoverageCannotCollectMoneyWithoutApplyingTheContract(){
            ledger.receive(id,receipt("coverage-start",10000));var request=new OrderChangeService.Request(HmRepository.number(order(),"version"),new OrderChangeService.Contact("C","13800000002","District B","B"),12000,"New address and price","coverage-change");
            changes.create(id,request,true,true);jdbc.update("DELETE FROM hm_worker_area");jdbc.update("INSERT INTO hm_worker_area VALUES(1,1,'A')");
            assertThrows(Exception.class,()->ledger.receive(id,receipt("invalid-area-money",2000)));assertEquals(10000,HmRepository.cents(order(),"paid_cents"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_payment_entry",Integer.class));
        }
        @Test void historicalAreaWithoutSnapshotRequiresExplicitRequoteForNewDistrict(){jdbc.update("UPDATE hm_order SET area_fee_cents=NULL WHERE id=?",id);assertThrows(Exception.class,()->changes.create(id,address("B"),true,false));assertDoesNotThrow(()->changes.create(id,address(""),true,false));}
        @Test void staleVersionsAndSecondPendingChangeAreRejected(){var old=price(12000,"old-version");changes.create(id,address(""),true,false);assertThrows(Exception.class,()->changes.create(id,old,true,true));ledger.receive(id,receipt("pending-start",10000));changes.create(id,price(12000,"first-pending"),true,true);assertThrows(Exception.class,()->changes.create(id,price(14000,"second-pending"),true,true));}
        @Test void otherStoreAndTenantCannotReadOrChangeTheOrder(){var request=address("");AdminScope.set(new AdminScope(1,false,"STORES",Set.of(2L),null));assertThrows(Exception.class,()->changes.create(id,request,true,false));assertThrows(Exception.class,()->orders.detail(id,true));AdminScope.set(null);TenantContextHolder.setTenantId(2L);assertThrows(Exception.class,()->orders.detail(id,false));}
        @Test void customerAddressMustBelongToTheSameCustomerAndTenant(){
            jdbc.update("INSERT INTO hm_customer(id,nickname) VALUES(2,'Other')");jdbc.update("INSERT INTO hm_customer_address(id,tenant_id,customer_id,contact_name,phone,address) VALUES(2,1,2,'Other','13800000002','Private'),(3,2,1,'Customer','13800000001','Other tenant')");customer();
            for(long address:List.of(2L,3L))assertThrows(Exception.class,()->changes.customerRequest(id,new OrderChangeService.CustomerRequest(0,address,"Change","customer-private")));
        }
        @Test void rescheduleRetainsTheOriginalAppointmentSnapshot(){
            var start=LocalDate.now().plusDays(2).atTime(13,0);orders.reschedule(id,new OrderService.Reschedule(start,1L,"Later appointment"),true);
            var history=changes.history(id).get(0);assertEquals("RESCHEDULE",history.get("kind"));assertEquals(start.toString(),((Map<?,?>)history.get("after")).get("starts_at"));assertNotEquals(((Map<?,?>)history.get("before")).get("starts_at"),((Map<?,?>)history.get("after")).get("starts_at"));
        }
        @Test void changedAreaQuoteMustBeConfirmedAgainInsteadOfChargingANewPrice(){
            jdbc.update("INSERT INTO hm_service_area(id,tenant_id,name,district_code,extra_cents) VALUES(1,1,'B','B',2000)");jdbc.update("INSERT INTO hm_service_area_relation VALUES(1,1,1)");var request=address("B");
            jdbc.update("UPDATE hm_service_area SET extra_cents=3000 WHERE id=1");assertThrows(Exception.class,()->changes.create(id,request,true,false));assertEquals(10000,HmRepository.cents(order(),"price_cents"));assertTrue(changes.history(id).isEmpty());
        }
        @Test void serviceWorkerCannotAcceptAnUnsettledAmendment(){
            jdbc.update("INSERT INTO hm_worker_account VALUES(1,1,42)");ledger.receive(id,receipt("worker-original",10000));changes.create(id,price(11000,"worker-pending"),true,true);
            assertThrows(Exception.class,()->workerService.action(id,new WorkerService.Action("ACCEPT","")));ledger.receive(id,receipt("worker-difference",1000));assertDoesNotThrow(()->workerService.action(id,new WorkerService.Action("ACCEPT","")));
        }
        @Test void simultaneousReceiptAndCancellationCannotLeaveAnUnbalancedContract()throws Exception{
            ledger.receive(id,receipt("race-original",10000));long change=changes.create(id,price(12000,"race-amendment"),true,true);var latch=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
            try{
                var collect=pool.submit(()->{admin();latch.await();try{ledger.receive(id,receipt("race-difference",2000));return true;}catch(Exception e){return false;}finally{clear();}});
                var cancel=pool.submit(()->{admin();latch.await();try{changes.cancel(id,change,"Cancel before settlement",true,true);return true;}catch(Exception e){return false;}finally{clear();}});
                latch.countDown();boolean received=collect.get(10,TimeUnit.SECONDS),cancelled=cancel.get(10,TimeUnit.SECONDS);assertNotEquals(received,cancelled);assertEquals(received?12000:10000,HmRepository.cents(order(),"price_cents"));assertEquals(HmRepository.cents(order(),"price_cents"),HmRepository.cents(summary(),"net_cents"));assertNull(order().get("pending_change_id"));
            }finally{pool.shutdownNow();}
        }
    }
}
