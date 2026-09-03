package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.dal.HmRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(BusinessIsolationTest.Config.class)
class BusinessIsolationTest {
    @Configuration @EnableTransactionManagement static class Config {
        @Bean static org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor exceptionTranslation(){return new org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor();}
        @Bean DataSource dataSource(){return new DriverManagerDataSource("jdbc:h2:mem:hm_business;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");}
        @Bean JdbcTemplate jdbcTemplate(DataSource d){return new JdbcTemplate(d);}
        @Bean DataSourceTransactionManager transactionManager(DataSource d){return new DataSourceTransactionManager(d);}
        @Bean HmRepository repository(JdbcTemplate j){return new HmRepository(j);}
        @Bean CustomerAccess customers(HmRepository r){return new CustomerAccess(r);}
        @Bean NotificationService notifications(){return mock(NotificationService.class);}
        @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper(){return new com.fasterxml.jackson.databind.ObjectMapper();}
        @Bean PricingService pricing(HmRepository r,CustomerAccess c,com.fasterxml.jackson.databind.ObjectMapper j){return new PricingService(r,c,j);}
        @Bean QuotaService quotas(HmRepository r,com.fasterxml.jackson.databind.ObjectMapper j){return new QuotaService(r,j);}
        @Bean ScheduleService schedules(HmRepository r,PricingService p,CustomerAccess c){return new ScheduleService(r,p,c);}
        @Bean SettlementService settlements(HmRepository r,QuotaService q){return new SettlementService(r,q);}
        @Bean OrderService orders(HmRepository r,CustomerAccess c,NotificationService n,PricingService p,ScheduleService s,QuotaService q,SettlementService x){return new OrderService(r,c,n,p,s,q,x);}
        @Bean OrderChangeService changes(HmRepository r,PricingService p,ScheduleService s,CustomerAccess c,PaymentPolicyService policy,com.fasterxml.jackson.databind.ObjectMapper j){return new OrderChangeService(r,p,s,c,policy,j);}
        @Bean CatalogService catalog(HmRepository r){return new CatalogService(r);}
        @Bean ServiceSettingsService serviceSettings(HmRepository r,PricingService p,com.fasterxml.jackson.databind.ObjectMapper j){return new ServiceSettingsService(r,p,j);}
        @Bean com.hm.module.pay.api.refund.PayRefundApi refundApi(){return mock(com.hm.module.pay.api.refund.PayRefundApi.class);}
        @Bean com.hm.module.pay.api.order.PayOrderApi payOrderApi(){return mock(com.hm.module.pay.api.order.PayOrderApi.class);}
        @Bean com.hm.module.pay.service.order.PayOrderService payOrderService(){return mock(com.hm.module.pay.service.order.PayOrderService.class);}
        @Bean com.hm.module.pay.service.app.PayAppService payApps(){return mock(com.hm.module.pay.service.app.PayAppService.class);}
        @Bean com.hm.module.pay.service.channel.PayChannelService payChannels(){return mock(com.hm.module.pay.service.channel.PayChannelService.class);}
        @Bean PaymentPolicyService paymentPolicy(HmRepository r,com.hm.module.pay.service.app.PayAppService a,com.hm.module.pay.service.channel.PayChannelService c){return new PaymentPolicyService(r,a,c,jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator());}
        @Bean PaymentLedgerService ledger(HmRepository r,OrderService o,SettlementService s,PaymentPolicyService p){return new PaymentLedgerService(r,o,s,p);}
        @Bean PaymentService payments(HmRepository r,CustomerAccess c,OrderService o,com.hm.module.pay.api.refund.PayRefundApi f,NotificationService n,SettlementService s,com.hm.module.pay.api.order.PayOrderApi a,com.hm.module.pay.service.order.PayOrderService p){return new PaymentService(r,c,o,a,f,p,n,s);}
        @Bean EvidenceStorage storage(){return mock(EvidenceStorage.class);}
        @Bean WorkerService workers(HmRepository r,OrderService o,EvidenceStorage f,CustomerAccess c){return new WorkerService(r,o,f,c);}
        @Bean PortalService portal(HmRepository r,com.fasterxml.jackson.databind.ObjectMapper j,QuotaService q){return new PortalService(r,j,q);}
    }
    @Autowired JdbcTemplate jdbc;@Autowired DataSource dataSource;@Autowired OrderService orders;@Autowired CatalogService catalog;@Autowired HmRepository repo;@Autowired ScheduleService schedules;
    @Autowired WorkerService workers;@Autowired SettlementService settlements;@Autowired PortalService portal;
    @Autowired ServiceSettingsService serviceSettings;@Autowired PaymentService payments;@Autowired com.hm.module.pay.api.refund.PayRefundApi refundApi;
    @BeforeEach void seed(){
        jdbc.execute("DROP ALL OBJECTS");
        var scripts=new ResourceDatabasePopulator(new FileSystemResource(Path.of("../sql/mysql/hm-homemaking.sql")),new FileSystemResource(Path.of("../sql/mysql/upgrades/V002__operations_and_portal.sql")));scripts.setSqlScriptEncoding("UTF-8");scripts.execute(dataSource);
        BusinessTestSchema.payment(dataSource);
        jdbc.update("INSERT INTO hm_customer(id,nickname) VALUES(1,'A'),(2,'B')");
        jdbc.update("INSERT INTO hm_customer_tenant(tenant_id,customer_id) VALUES(1,1),(1,2),(2,2)");
        jdbc.update("INSERT INTO hm_store(id,tenant_id,name) VALUES(1,1,'HQ'),(2,2,'Franchise')");
        jdbc.update("INSERT INTO hm_worker(id,tenant_id,store_id,name) VALUES(1,1,1,'Alice'),(2,2,2,'Bob')");
        jdbc.update("INSERT INTO hm_service(id,tenant_id,store_id,name,description,price_cents,duration_minutes) VALUES(1,1,1,'Clean','',10000,60),(2,2,2,'Clean','',20000,60)");
        jdbc.update("INSERT INTO hm_customer_address(id,tenant_id,customer_id,contact_name,phone,address) VALUES(1,1,1,'A','13800000001','Address A'),(2,1,2,'B','13800000002','Address B'),(3,2,2,'B','13800000002','Address C')");
        jdbc.update("INSERT INTO hm_worker_skill VALUES(1,1,1),(2,2,2)");
        jdbc.update("INSERT INTO hm_worker_area VALUES(1,1,'*'),(2,2,'*')");
        jdbc.update("INSERT INTO hm_worker_schedule(tenant_id,worker_id,starts_at,ends_at) VALUES(1,1,?,?),(2,2,?,?)",slot().toLocalDate().atStartOfDay(),slot().toLocalDate().atTime(23,30),slot().toLocalDate().atStartOfDay(),slot().toLocalDate().atTime(23,30));
        login(1,1);
    }
    @AfterEach void clear(){TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    void login(long tenant,long customer){TenantContextHolder.setTenantId(tenant);TenantContextHolder.setIgnore(false);var u=new LoginUser();u.setId(customer);u.setUserType(1);u.setTenantId(tenant);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
    LocalDateTime slot(){return LocalDate.now().plusDays(2).atTime(9,0);}
    OrderService.Book request(String key,long address){return new OrderService.Book(1L,1L,slot(),address,key);}
    @Test void legacyCategoriesAndHomepageSettingsRemainStable(){
        jdbc.update("INSERT INTO hm_service_category(id,tenant_id,name) VALUES(9,1,'家政服务'),(10,1,'A category'),(11,2,'Foreign')");
        jdbc.update("UPDATE hm_tenant_profile SET home_modules='[\"reviews\",\"contact\"]' WHERE tenant_id=1");
        var controller=new com.hm.module.homemaking.controller.app.LegacyMiniController(repo,orders,null,null,new com.fasterxml.jackson.databind.ObjectMapper());
        var home=(Map<String,Object>)controller.home().getData();
        assertEquals(List.of("reviews","contact"),home.get("homeModules"));
        var categories=(List<Map<String,Object>>)home.get("categoryList");assertEquals(9L,((Number)categories.get(0).get("categoryId")).longValue());assertEquals(1,categories.size());
        var services=(Map<String,Object>)controller.services(1,10,9,"").getData();assertEquals(1L,services.get("total"));
        var foreign=(Map<String,Object>)controller.services(1,10,11,"").getData();assertEquals(0,foreign.get("total"));
    }
    @Test void tenantCannotReadOrUseAnotherTenantsService(){login(2,2);assertThrows(Exception.class,()->repo.require("hm_service",1,false));assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,null,slot(),3L,"cross")));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order",Integer.class));}
    @Test void addressMustBelongToCurrentCustomer(){assertThrows(Exception.class,()->orders.book(request("address",2)));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order",Integer.class));}
    @Test void bookingSnapshotsServerPriceAndIsIdempotent(){long id=orders.book(request("repeat",1));assertEquals(id,orders.book(request("repeat",1)));assertEquals(10000,jdbc.queryForObject("SELECT price_cents FROM hm_order WHERE id=?",Integer.class,id));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Integer.class));}
    @Test void overlappingReservationRollsBackAllBusinessRows(){orders.book(request("first",1));login(1,2);assertThrows(Exception.class,()->orders.book(request("second",2)));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_booking",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order",Integer.class));}
    @Test void customerCannotReadAnotherCustomersOrder(){long id=orders.book(request("owner",1));login(1,2);assertThrows(Exception.class,()->orders.detail(id,false));}
    @Test void cancellationReleasesSlotsAndCannotCompleteUnpaidOrder(){long id=orders.book(request("cancel",1));assertThrows(Exception.class,()->orders.complete(id));orders.cancel(id,false);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Integer.class));assertEquals("CANCELLED",jdbc.queryForObject("SELECT status FROM hm_order WHERE id=?",String.class,id));}
    @Test void publicWorkerDirectoryDoesNotExposePhone(){var page=catalog.list("workers",1,20,true);var row=((List<Map<String,Object>>)page.get("list")).get(0);assertFalse(row.containsKey("phone"));assertEquals(1L,((Number)row.get("id")).longValue());}
    @Test void cannotReviewAnUnfinishedOrder(){long id=orders.book(request("review",1));assertThrows(Exception.class,()->orders.review(new OrderService.Review(id,5,"Good")));}
    @Test void missingTenantFailsClosed(){TenantContextHolder.clear();assertThrows(Exception.class,()->catalog.list("stores",1,10,true));}
    @Test void skuCannotBeBorrowedFromAnotherTenant(){jdbc.update("INSERT INTO hm_service_sku(id,tenant_id,service_id,name,price_cents,duration_minutes) VALUES(1,2,2,'Foreign',1,60)");assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,null,slot(),1L,"sku",1L,List.of(),"")));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order",Integer.class));}
    @Test void extrasAndAreaFeeAreCalculatedOnServer(){jdbc.update("INSERT INTO hm_service_extra(id,tenant_id,service_id,name,price_cents) VALUES(1,1,1,'Windows',1500)");jdbc.update("INSERT INTO hm_service_area(id,tenant_id,name,district_code,extra_cents) VALUES(1,1,'District','110101',500)");jdbc.update("INSERT INTO hm_service_area_relation VALUES(1,1,1)");jdbc.update("UPDATE hm_customer_address SET district_code='110101' WHERE id=1");long id=orders.book(new OrderService.Book(1L,null,slot(),1L,"price",null,List.of(new PricingService.Extra(1L,2)),""));assertEquals(13500,jdbc.queryForObject("SELECT price_cents FROM hm_order WHERE id=?",Integer.class,id));assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order_item WHERE order_id=?",Integer.class,id));}
    @Test void negativeAddonQuantityIsRejected(){assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,null,slot(),1L,"negative",null,List.of(new PricingService.Extra(1L,-1)),"")));}
    @Test void bookingMustUseConfiguredTime(){assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,null,slot().plusMinutes(30),1L,"slot")));}
    @Test void bookingCannotFinishAfterClosingTime(){jdbc.update("UPDATE hm_service SET duration_minutes=330 WHERE id=1");assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,null,slot().withHour(16),1L,"late")));}
    @Test void availabilityRequiresSkillAreaScheduleAndNoLeave(){
        assertTrue(schedules.capacity(1L,null,1L,slot().toLocalDate(),1L).stream().anyMatch(row->Boolean.TRUE.equals(row.get("available"))));
        jdbc.update("DELETE FROM hm_worker_skill WHERE tenant_id=1 AND worker_id=1");
        assertTrue(schedules.capacity(1L,null,1L,slot().toLocalDate(),1L).stream()
                .filter(row->slot().equals(row.get("startsAt")))
                .noneMatch(row->Boolean.TRUE.equals(row.get("available"))));
        jdbc.update("INSERT INTO hm_worker_skill VALUES(1,1,1)");
        schedules.add(1L,new ScheduleService.Interval(slot().minusMinutes(30),slot().plusHours(2),"请假"),true);
        var capacity=schedules.capacity(1L,null,1L,slot().toLocalDate(),1L);
        assertEquals(false,capacity.stream().filter(row->slot().equals(row.get("startsAt"))).findFirst().orElseThrow().get("available"));
        assertTrue(capacity.stream().anyMatch(row->Boolean.TRUE.equals(row.get("available"))));
    }
    @Test void failedRescheduleKeepsOriginalReservation(){
        long id=orders.book(request("move",1));
        LocalDateTime original=slot();
        assertThrows(Exception.class,()->orders.reschedule(id,new OrderService.Reschedule(original.plusDays(1),1L,"客户改期"),false));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot WHERE booking_id=(SELECT booking_id FROM hm_order WHERE id=?)",Integer.class,id));
        assertEquals(original,jdbc.queryForObject("SELECT starts_at FROM hm_booking WHERE id=(SELECT booking_id FROM hm_order WHERE id=?)",LocalDateTime.class,id));
    }
    @Test void tenantOrderQuotaStopsTheNextCreate(){
        jdbc.update("INSERT INTO hm_tenant_entitlement(tenant_id,limits_json,features_json) VALUES(1,'{\"orders\":1}','{}')");
        orders.book(request("quota-one",1));
        assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,1L,slot().withHour(13),1L,"quota-two")));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order WHERE tenant_id=1",Integer.class));
    }
    @Test void workerCannotTurnUnpaidReservationIntoPaidOrder(){
        long id=orders.book(request("worker-unpaid",1));
        jdbc.update("INSERT INTO hm_worker_account VALUES(1,1,11)");login(1,11);
        ((LoginUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).setUserType(2);
        assertThrows(Exception.class,()->workers.action(id,new WorkerService.Action("REJECT","无法到场")));
        assertEquals("UNPAID",jdbc.queryForObject("SELECT status FROM hm_order WHERE id=?",String.class,id));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Integer.class));
    }
    @Test void workerAccountCannotSilentlyBindToTwoProfiles(){
        jdbc.execute("CREATE TABLE system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted BOOLEAN,status INT)");
        jdbc.update("INSERT INTO system_users VALUES(11,1,FALSE,0)");
        jdbc.update("INSERT INTO hm_worker(id,tenant_id,store_id,name) VALUES(3,1,1,'Second')");
        workers.bind(1,new WorkerService.Binding(11L));
        assertThrows(Exception.class,()->workers.bind(3,new WorkerService.Binding(11L)));
        assertEquals(1L,jdbc.queryForObject("SELECT worker_id FROM hm_worker_account WHERE user_id=11",Long.class));
    }
    @Test void unboundWorkerCannotReadEvidence(){
        long id=orders.book(request("evidence-owner",1));login(1,11);
        ((LoginUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).setUserType(2);
        assertThrows(Exception.class,()->workers.evidence(id,false));
    }
    @Test void partialRefundsAndRepeatedCallbacksBalanceEveryBeneficiary(){
        long id=orders.book(request("ledger",1));
        jdbc.update("UPDATE hm_order SET paid_cents=101 WHERE id=?",id);
        jdbc.update("INSERT INTO hm_commission_rule(tenant_id,platform_bps,worker_bps,enabled) VALUES(1,3333,3333,TRUE)");
        var order=repo.require("hm_order",id,false);settlements.completed(order);
        settlements.refund(order,11,50);settlements.refund(order,11,50);settlements.refund(order,12,51);
        var allocation=jdbc.queryForMap("SELECT * FROM hm_settlement_allocation WHERE order_id=?",id);
        for(String key:List.of("net_cents","platform_cents","worker_cents","store_cents"))assertEquals(0L,((Number)allocation.get(key)).longValue());
        for(String type:List.of("PLATFORM","STORE","WORKER"))assertEquals(0L,jdbc.queryForObject("SELECT SUM(amount_cents) FROM hm_settlement_entry WHERE beneficiary=?",Long.class,type));
    }
    @Test void portalDraftIsSeparateAndRejectsForeignRecommendations(){
        var config=(PortalService.Config)portal.draft().get("config");portal.save(new PortalService.Draft(config,0));
        assertNull(jdbc.queryForObject("SELECT published_json FROM hm_portal_site WHERE tenant_id=1",String.class));
        portal.publish(new PortalService.Publish(1));portal.publish(new PortalService.Publish(1));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_portal_revision",Integer.class));
        assertThrows(Exception.class,()->portal.save(new PortalService.Draft(config,0)));
        var foreign=new PortalService.Config(config.seoTitle(),config.seoDescription(),config.announcement(),config.navigation(),config.heroTitle(),config.heroText(),config.heroImage(),config.primaryAction(),config.secondaryAction(),List.of(new PortalService.Module("SERVICES","服务","",true,List.of(2L),List.of())),config.footerText());
        assertThrows(Exception.class,()->portal.save(new PortalService.Draft(foreign,1)));
        assertThrows(Exception.class,()->CatalogService.safeUrl("//example.invalid/image.png"));
        assertThrows(Exception.class,()->CatalogService.safeUrl("javascript:alert(1)"));
    }
    @Test void racingCustomersCanReserveOnlyOneWorkerSlot() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);var ready=new java.util.concurrent.CountDownLatch(2);var start=new java.util.concurrent.CountDownLatch(1);
        try{
            var tasks=new ArrayList<java.util.concurrent.Future<Boolean>>();
            for(long customer=1;customer<=2;customer++){long c=customer;tasks.add(executor.submit(()->{login(1,c);ready.countDown();start.await();try{orders.book(request("race-"+c,c));return true;}catch(org.springframework.web.server.ResponseStatusException ex){return false;}finally{clear();}}));}
            assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));start.countDown();int succeeded=0;for(var task:tasks)if(task.get(10,java.util.concurrent.TimeUnit.SECONDS))succeeded++;
            assertEquals(1,succeeded);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Integer.class));
        }finally{executor.shutdownNow();}
    }
    @Test void serviceSettingsUpdateServerPriceAndPreserveHistoricalOrderSnapshot(){
        long oldOrder=orders.book(request("before-settings",1));var original=serviceSettings.get(1);
        var settings=new ServiceSettingsService.Settings(original.version(),List.of(new ServiceSettingsService.Option(null,"Premium",15000,90,true)),List.of(new ServiceSettingsService.Option(null,"Windows",500,30,true)),List.of(new ServiceSettingsService.Area(null,"District","110101",800)),original.rule());
        serviceSettings.save(1,settings);var saved=serviceSettings.get(1);assertEquals(1,saved.version());
        assertThrows(Exception.class,()->serviceSettings.save(1,settings));
        assertEquals(10000,jdbc.queryForObject("SELECT price_cents FROM hm_order WHERE id=?",Integer.class,oldOrder));
        jdbc.update("UPDATE hm_customer_address SET district_code='110101' WHERE id=1");
        long next=orders.book(new OrderService.Book(1L,null,slot().withHour(13),1L,"configured",saved.skus().get(0).id(),List.of(new PricingService.Extra(saved.extras().get(0).id(),2)),""));
        assertEquals(16800,jdbc.queryForObject("SELECT price_cents FROM hm_order WHERE id=?",Integer.class,next));
    }
    @Test void foreignServiceOptionRollsBackTheWholeSettingsSave(){
        jdbc.update("INSERT INTO hm_service_sku(id,tenant_id,service_id,name,price_cents,duration_minutes) VALUES(11,1,1,'Local',10000,60),(12,2,2,'Foreign',20000,60)");
        var original=serviceSettings.get(1);
        var attack=new ServiceSettingsService.Settings(0,List.of(new ServiceSettingsService.Option(12L,"Forged",1,60,true)),List.of(),List.of(),original.rule());
        assertThrows(Exception.class,()->serviceSettings.save(1,attack));
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM hm_service_sku WHERE id=11",String.class));
        assertEquals(20000,jdbc.queryForObject("SELECT price_cents FROM hm_service_sku WHERE id=12",Integer.class));
        assertEquals(0,serviceSettings.get(1).version());
    }
    @Test void editingSharedImportedAreaDoesNotChangeAnotherService(){
        jdbc.update("INSERT INTO hm_service(id,tenant_id,store_id,name,description,price_cents,duration_minutes) VALUES(3,1,1,'Other','',10000,60)");
        jdbc.update("INSERT INTO hm_service_area(id,tenant_id,name,district_code,extra_cents) VALUES(10,1,'Shared','110101',500)");
        jdbc.update("INSERT INTO hm_service_area_relation VALUES(1,1,10),(1,3,10)");
        var original=serviceSettings.get(1);serviceSettings.save(1,new ServiceSettingsService.Settings(0,List.of(),List.of(),List.of(new ServiceSettingsService.Area(10L,"Shared","110101",900)),original.rule()));
        assertEquals(900,serviceSettings.get(1).areas().get(0).extraCents());assertEquals(500,serviceSettings.get(3).areas().get(0).extraCents());
        assertNotEquals(serviceSettings.get(1).areas().get(0).id(),serviceSettings.get(3).areas().get(0).id());
    }
    @Test void refundCallbackUsesPersistedTenantChecksMerchantAndIsIdempotent(){
        long id=orders.book(request("refund-callback",1));jdbc.update("UPDATE hm_order SET paid_cents=price_cents,status='REFUNDING' WHERE id=?",id);
        long a=repo.insert("INSERT INTO hm_aftersale(tenant_id,customer_id,order_id,amount_cents,reason,status,pay_refund_id,previous_order_status) VALUES(1,1,?,10000,'Cancelled','REFUNDING',99,'PAID')",id);
        jdbc.execute("CREATE TABLE pay_refund(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted BOOLEAN)");jdbc.update("INSERT INTO pay_refund VALUES(99,1,FALSE)");
        var refund=new com.hm.module.pay.api.refund.dto.PayRefundRespDTO();refund.setStatus(com.hm.module.pay.enums.refund.PayRefundStatusEnum.SUCCESS.getStatus());refund.setRefundPrice(10000);refund.setMerchantOrderId("HM-2-"+id);refund.setMerchantRefundId("HM-R-1-"+a);
        org.mockito.Mockito.when(refundApi.getRefund(99L)).thenReturn(refund);
        login(2,2);assertThrows(Exception.class,()->payments.refundCallback(99));
        assertEquals(0,jdbc.queryForObject("SELECT refunded_cents FROM hm_order WHERE id=?",Integer.class,id));
        refund.setMerchantOrderId("HM-1-"+id);payments.refundCallback(99);payments.refundCallback(99);
        assertEquals(2L,TenantContextHolder.getTenantId());assertEquals(10000,jdbc.queryForObject("SELECT refunded_cents FROM hm_order WHERE id=?",Integer.class,id));
        assertEquals("REFUNDED",jdbc.queryForObject("SELECT status FROM hm_aftersale WHERE id=?",String.class,a));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order_log WHERE order_id=? AND action='REFUNDED'",Integer.class,id));
    }

}
