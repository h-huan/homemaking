package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.AdminScope;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.hm.module.homemaking.dal.HmRepository.number;

@SpringJUnitConfig(BusinessIsolationTest.Config.class)
class WorkerWorkbenchTest {
    @Autowired JdbcTemplate jdbc; @Autowired DataSource dataSource; @Autowired HmRepository repo;
    @Autowired WorkerTimeOffService leave; @Autowired WorkerWorkbenchService workbench;
    @Autowired ScheduleService schedules; @Autowired OrderService orders;
    @Autowired PaymentLedgerService ledger; @Autowired SettlementService settlements;
    @Autowired CompletionConfirmationService completionConfirmation;
    LocalDate date; LocalDateTime start;
    @BeforeEach void seed() {
        var fixture=new BusinessIsolationTest();fixture.jdbc=jdbc;fixture.dataSource=dataSource;fixture.seed();
        date=LocalDate.now().plusDays(2);start=date.atTime(9,0);
        jdbc.update("INSERT INTO hm_worker(id,tenant_id,store_id,name) VALUES(3,1,1,'Another worker')");
        jdbc.update("INSERT INTO hm_worker_account VALUES(1,1,11),(1,3,12),(2,2,22)");
        jdbc.execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,nickname VARCHAR(40))");jdbc.update("INSERT INTO system_users VALUES(42,1,'Finance')");
        worker();
    }
    @AfterEach void clear(){AdminScope.set(null);SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void login(long tenant,long user,int type){AdminScope.set(null);TenantContextHolder.setTenantId(tenant);TenantContextHolder.setIgnore(false);var u=new LoginUser();u.setId(user);u.setTenantId(tenant);u.setUserType(type);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
    void worker(){login(1,11,2);}
    void admin(){login(1,42,2);AdminScope.set(new AdminScope(1,false,"TENANT",Set.of(),null));}
    WorkerTimeOffService.Request request(String key){return new WorkerTimeOffService.Request(start,start.plusHours(2),"LEAVE","Personal appointment",key);}
    Map<String,Object> row(long id){return jdbc.queryForMap("SELECT * FROM hm_worker_leave WHERE id=?",id);}
    List<Map<String,Object>> free(){return (List<Map<String,Object>>)workbench.calendar(date,date).get("available");}
    boolean freeAt(LocalDateTime at){return free().stream().anyMatch(r->!at.isBefore(OrderService.time(r.get("starts_at")))&&at.isBefore(OrderService.time(r.get("ends_at"))));}
    Map<String,Object> income(String view){return workbench.income(LocalDate.now(),LocalDate.now(),view,1,20);}
    Map<String,Object> summary(){return (Map<String,Object>)income("ENTRIES").get("summary");}
    @Test void pendingApprovalCancellationAndAuditFormOneCapacityLifecycle(){
        long id=workbench.request(request("lifecycle"));assertEquals("PENDING",row(id).get("status"));assertTrue(freeAt(start));
        assertEquals(id,workbench.request(request("lifecycle")));assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_leave_log",Long.class));
        admin();leave.review(1,id,new WorkerTimeOffService.Decision(0,true,"Coverage arranged"));worker();assertFalse(freeAt(start));assertTrue(freeAt(start.plusHours(2)));
        assertThrows(Exception.class,()->workbench.cancel(id,new WorkerTimeOffService.Cancel(0,"Stale view")));
        workbench.cancel(id,new WorkerTimeOffService.Cancel(1,"Appointment cancelled"));assertEquals("CANCELLED",row(id).get("status"));assertTrue(freeAt(start));
        assertEquals(List.of("REQUEST","APPROVE","CANCEL"),jdbc.queryForList("SELECT action FROM hm_worker_leave_log ORDER BY id",String.class));
        assertEquals(List.of(11L,42L,11L),jdbc.queryForList("SELECT operator_id FROM hm_worker_leave_log ORDER BY id",Long.class));
        var item=leave.list(1,date,date).get(0);assertFalse(item.containsKey("request_hash"));assertFalse(item.containsKey("request_key"));assertEquals(3,((List<?>)item.get("history")).size());
    }
    @Test void restRejectAndRetryPreserveHistory(){
        var rest=new WorkerTimeOffService.Request(start,start.plusDays(1),"REST","Family day","rest");long id=workbench.request(rest);
        admin();leave.review(1,id,new WorkerTimeOffService.Decision(0,false,"Coverage unavailable"));worker();
        assertTrue(freeAt(start));assertEquals("REST",row(id).get("kind"));assertThrows(Exception.class,()->workbench.cancel(id,new WorkerTimeOffService.Cancel(1,"Cannot undo rejection")));
        long retry=workbench.request(new WorkerTimeOffService.Request(rest.startsAt(),rest.endsAt(),rest.kind(),rest.reason(),"retry"));assertNotEquals(id,retry);
    }
    @Test void duplicateKeyChangedPayloadAndOverlappingRequestsAreRejected(){
        workbench.request(request("same"));assertThrows(Exception.class,()->workbench.request(new WorkerTimeOffService.Request(start,start.plusHours(3),"LEAVE","Changed","same")));
        assertThrows(Exception.class,()->workbench.request(request("overlap")));assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_leave",Long.class));
    }
    @Test void bookingBlocksApprovalButNotRequestAndPendingCanBeCancelled(){
        login(1,1,1);orders.book(new OrderService.Book(1L,1L,start,1L,"occupied"));worker();long id=workbench.request(request("after-booking"));
        admin();assertThrows(Exception.class,()->leave.review(1,id,new WorkerTimeOffService.Decision(0,true,"Must reassign")));
        assertEquals("PENDING",row(id).get("status"));assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_leave_log",Long.class));
        worker();workbench.cancel(id,new WorkerTimeOffService.Cancel(0,"Keep booking"));assertFalse(freeAt(start));
    }
    @Test void approvedLeaveBlocksNewBookingAndCancellationReopensIt(){
        long id=workbench.request(request("booking-gate"));admin();leave.review(1,id,new WorkerTimeOffService.Decision(0,true,"Approved"));
        login(1,1,1);assertThrows(Exception.class,()->orders.book(new OrderService.Book(1L,1L,start,1L,"blocked")));
        worker();workbench.cancel(id,new WorkerTimeOffService.Cancel(1,"Available again"));login(1,1,1);assertDoesNotThrow(()->orders.book(new OrderService.Book(1L,1L,start,1L,"available")));
    }
    @Test void bookingAndApprovalSerializeOnTheWorkerLock() throws Exception {
        long id=workbench.request(request("race"));var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> booking=pool.submit(()->{login(1,1,1);ready.countDown();go.await();try{orders.book(new OrderService.Book(1L,1L,start,1L,"race-booking"));return true;}catch(Exception e){return false;}finally{clear();}});
            Future<Boolean> approval=pool.submit(()->{admin();ready.countDown();go.await();try{leave.review(1,id,new WorkerTimeOffService.Decision(0,true,"Concurrent approval"));return true;}catch(Exception e){return false;}finally{clear();}});
            assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();assertNotEquals(booking.get(15,TimeUnit.SECONDS),approval.get(15,TimeUnit.SECONDS));
            assertFalse("APPROVED".equals(row(id).get("status"))&&jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Long.class)>0);
        } finally {pool.shutdownNow();}
    }
    @Test void legacyAdminRegistrationUsesAuditAndSoftCancellation(){
        admin();long id=schedules.add(1,new ScheduleService.Interval(start,start.plusHours(1),"Manual approved leave"),true);assertEquals("APPROVED",row(id).get("status"));
        schedules.remove(1,id,true);assertEquals("CANCELLED",row(id).get("status"));assertEquals(List.of("REGISTER","CANCEL"),jdbc.queryForList("SELECT action FROM hm_worker_leave_log ORDER BY id",String.class));
    }
    @Test void elapsedApprovedLeaveCannotBeReopenedButExpiredPendingCanBeCancelled(){
        long id=workbench.request(request("elapsed"));admin();leave.review(1,id,new WorkerTimeOffService.Decision(0,true,"Approved"));jdbc.update("UPDATE hm_worker_leave SET starts_at=? WHERE id=?",LocalDateTime.now().minusHours(1),id);
        worker();assertThrows(Exception.class,()->workbench.cancel(id,new WorkerTimeOffService.Cancel(1,"Too late")));
        long pending=workbench.request(new WorkerTimeOffService.Request(start.plusDays(1),start.plusDays(1).plusHours(1),"REST","Later rest","elapsed-pending"));jdbc.update("UPDATE hm_worker_leave SET starts_at=? WHERE id=?",LocalDateTime.now().minusHours(1),pending);
        workbench.cancel(pending,new WorkerTimeOffService.Cancel(0,"Expired"));assertEquals("CANCELLED",row(pending).get("status"));
    }
    @Test void invalidIntervalsAndReasonCannotCreateRows(){
        for(var r:List.of(new WorkerTimeOffService.Request(start,start,"REST","Same time","a"),new WorkerTimeOffService.Request(start.plusMinutes(1),start.plusHours(1),"LEAVE","Unaligned","b"),new WorkerTimeOffService.Request(start,start.plusDays(32),"LEAVE","Too long","c"),new WorkerTimeOffService.Request(start,start.plusHours(1),"LEAVE"," ","d")))assertThrows(Exception.class,()->workbench.request(r));
        assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_leave",Long.class));
    }
    @Test void selfQueriesAndCancellationRejectAnotherWorkerTenantCustomerAndDisabledAccount(){
        long id=workbench.request(request("private"));login(1,12,2);assertThrows(Exception.class,()->workbench.cancel(id,new WorkerTimeOffService.Cancel(0,"Other worker")));assertTrue(((List<?>)workbench.calendar(date,date).get("leaves")).isEmpty());
        login(2,22,2);assertThrows(Exception.class,()->workbench.cancel(id,new WorkerTimeOffService.Cancel(0,"Foreign tenant")));assertTrue(((List<?>)workbench.calendar(date,date).get("leaves")).isEmpty());
        login(1,11,1);assertThrows(Exception.class,()->income("ENTRIES"));worker();jdbc.update("UPDATE hm_worker SET status='DISABLED' WHERE id=1");assertThrows(Exception.class,()->workbench.calendar(date,date));
    }
    @Test void noCommissionMeansNoInventedIncome(){assertEquals(false,income("ENTRIES").get("commissionConfigured"));assertEquals(0L,number(summary(),"pending_cents"));assertTrue(((List<?>)income("PAID").get("list")).isEmpty());}
    @Test void offlineReceiptCompletionPayoutAndRefundProduceSignedWorkerIncome(){
        jdbc.update("INSERT INTO hm_commission_rule(tenant_id,platform_bps,worker_bps,enabled) VALUES(1,1000,6000,TRUE)");
        login(1,1,1);long order=orders.book(new OrderService.Book(1L,1L,start,1L,"income-order"));admin();
        ledger.receive(order,new PaymentLedgerService.Receipt("CASH",10000,LocalDateTime.now().withNano(0),"Cash received","income-paid"));
        jdbc.update("UPDATE hm_order SET status='IN_SERVICE',fulfillment_status='STARTED' WHERE id=?",order);
        jdbc.update("INSERT INTO hm_fulfillment_evidence(tenant_id,order_id,worker_id,storage_key,content_type,phase,note) VALUES(1,?,1,'test-photo','image/jpeg','AFTER','fixture')",order);orders.complete(order);login(1,1,1);completionConfirmation.confirmByCustomer(order);admin();
        worker();assertEquals(6000L,number(summary(),"pending_cents"));assertEquals(6000L,number(summary(),"range_earned_cents"));
        admin();long statement=settlements.statement(new SettlementService.Statement(1,"WORKER",1,LocalDate.now(),LocalDate.now()));settlements.approve(statement);
        worker();assertEquals(6000L,number(summary(),"pending_cents"));admin();settlements.paid(statement,new SettlementService.Payout("worker-payout"));settlements.reconcile(statement);
        worker();assertEquals(0L,number(summary(),"pending_cents"));assertEquals(6000L,number(summary(),"paid_cents"));assertEquals(1L,number(income("PAID"),"total"));
        login(1,1,1);long refund=orders.aftersale(new OrderService.Aftersale(order,2000,"Partial refund"));admin();ledger.refund(refund,new PaymentLedgerService.Receipt("CASH",2000,LocalDateTime.now().withNano(0),"Cash returned","income-refund"));
        worker();assertEquals(-1200L,number(summary(),"pending_cents"));assertEquals(4800L,number(summary(),"range_earned_cents"));assertEquals(6000L,number(summary(),"paid_cents"));assertEquals(-1200L,number(((List<Map<String,Object>>)income("PENDING").get("list")).get(0),"amount_cents"));
        login(1,12,2);assertEquals(0L,number(summary(),"paid_cents"));assertEquals(0L,number(income("ENTRIES"),"total"));login(2,22,2);assertEquals(0L,number(income("PAID"),"total"));
    }
    @Test void pendingIncludesOlderUnpaidEntriesAndPaginationHasConsistentTotals(){
        for(int i=1;i<=25;i++)jdbc.update("INSERT INTO hm_settlement_entry(tenant_id,order_id,beneficiary,beneficiary_id,amount_cents,event_key,created_at) VALUES(1,?,'WORKER',1,100,?,?)",i,"older:"+i,LocalDateTime.now().minusMonths(2));
        var first=income("PENDING");assertEquals(25L,number(first,"total"));assertEquals(20,((List<?>)first.get("list")).size());assertEquals(2500L,number(summary(),"pending_cents"));assertEquals(0L,number(income("ENTRIES"),"total"));
        assertEquals(5,((List<?>)workbench.income(LocalDate.now(),LocalDate.now(),"PENDING",2,20).get("list")).size());
    }
}
