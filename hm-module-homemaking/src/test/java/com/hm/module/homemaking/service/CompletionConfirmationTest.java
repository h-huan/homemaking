package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.dal.HmRepository;
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
import static org.mockito.Mockito.*;

@SpringJUnitConfig(BusinessIsolationTest.Config.class)
class CompletionConfirmationTest {
    @Autowired JdbcTemplate jdbc;@Autowired DataSource dataSource;@Autowired OrderService orders;@Autowired CompletionConfirmationService completion;
    @Autowired NotificationService notifications;@Autowired HmRepository repo;
    long order;
    @BeforeEach void seed(){var fixture=new BusinessIsolationTest();fixture.jdbc=jdbc;fixture.dataSource=dataSource;fixture.seed();reset(notifications);customer(1);order=orders.book(new OrderService.Book(1L,1L,LocalDate.now().plusDays(2).atTime(9,0),1L,"completion"));jdbc.update("UPDATE hm_order SET status='IN_SERVICE',paid_cents=price_cents,fulfillment_status='STARTED' WHERE id=?",order);jdbc.update("INSERT INTO hm_fulfillment_evidence(tenant_id,order_id,worker_id,storage_key,content_type,phase,note) VALUES(1,?,1,'after','image/jpeg','AFTER','done')",order);}
    @AfterEach void clear(){SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void customer(long id){TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);var u=new LoginUser();u.setId(id);u.setTenantId(1L);u.setUserType(1);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
    Map<String,Object> row(){return repo.require("hm_order",order,false);}
    void submit(){orders.complete(order);}
    @Test void workerSubmissionWaitsForCustomerAndDoesNotSettle(){submit();var r=row();assertEquals("IN_SERVICE",r.get("status"));assertEquals("AWAITING_CONFIRMATION",r.get("fulfillment_status"));assertNotNull(r.get("worker_completed_at"));assertNotNull(r.get("confirmation_deadline"));assertNull(r.get("completed_at"));assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_settlement",Long.class));assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_worker_slot",Long.class));assertEquals("AWAITING_CONFIRMATION",jdbc.queryForObject("SELECT status FROM hm_booking WHERE id=(SELECT booking_id FROM hm_order WHERE id=?)",String.class,order));verify(notifications).enqueue(eq(1L),eq(order),eq("SERVICE_COMPLETION_CONFIRM"),anyString(),anyString(),anyMap());}
    @Test void customerConfirmationCompletesAndSettlesExactlyOnce(){submit();completion.confirmByCustomer(order);completion.confirmByCustomer(order);var r=row();assertEquals("COMPLETED",r.get("status"));assertEquals("CUSTOMER",r.get("completion_method"));assertEquals(1L,r.get("completion_confirmed_by"));assertNotNull(r.get("completion_confirmed_at"));assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_settlement",Long.class));assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order_log WHERE action='COMPLETION_CONFIRMED'",Long.class));verify(notifications,times(1)).enqueue(eq(1L),eq(order),eq("SERVICE_COMPLETED"),anyString(),anyString(),anyMap());}
    @Test void anotherCustomerCannotConfirmOrSeeTheOrder(){submit();customer(2);assertThrows(Exception.class,()->completion.confirmByCustomer(order));assertEquals("IN_SERVICE",rowForSystem().get("status"));}
    Map<String,Object> rowForSystem(){customer(1);return row();}
    @Test void timeoutOnlyCompletesDueOrders(){submit();completion.confirmTimeout(1,order);assertEquals("IN_SERVICE",row().get("status"));jdbc.update("UPDATE hm_order SET confirmation_deadline=? WHERE id=?",LocalDateTime.now().minusSeconds(1),order);completion.timeouts();assertEquals("COMPLETED",row().get("status"));assertEquals("TIMEOUT",row().get("completion_method"));assertNull(row().get("completion_confirmed_by"));}
    @Test void activeAftersalePausesCustomerAndTimeoutConfirmation(){submit();long aftersale=orders.aftersale(new OrderService.Aftersale(order,1000,"Service issue"));assertTrue(aftersale>0);assertThrows(Exception.class,()->completion.confirmByCustomer(order));jdbc.update("UPDATE hm_order SET confirmation_deadline=? WHERE id=?",LocalDateTime.now().minusSeconds(1),order);completion.timeouts();assertEquals("IN_SERVICE",row().get("status"));assertEquals("AWAITING_CONFIRMATION",row().get("fulfillment_status"));}
    @Test void cannotSubmitTwiceOrWithoutAfterEvidence(){submit();assertThrows(Exception.class,()->submit());jdbc.update("DELETE FROM hm_fulfillment_evidence");jdbc.update("UPDATE hm_order SET status='IN_SERVICE',fulfillment_status='STARTED' WHERE id=?",order);assertThrows(Exception.class,()->submit());}
    @Test void tenantConfirmationWindowIsValidatedAndUsed(){jdbc.update("UPDATE hm_tenant_profile SET completion_confirm_hours=2 WHERE tenant_id=1");var before=LocalDateTime.now().plusHours(2).minusMinutes(1);submit();var deadline=OrderService.time(row().get("confirmation_deadline"));assertTrue(deadline.isAfter(before)&&deadline.isBefore(before.plusMinutes(2)));}
    @Test void simultaneousCustomerConfirmationsSettleOnce() throws Exception {submit();var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{var calls=new ArrayList<Future<?>>();for(int i=0;i<2;i++)calls.add(pool.submit(()->{customer(1);ready.countDown();go.await();completion.confirmByCustomer(order);return null;}));assertTrue(ready.await(5,TimeUnit.SECONDS));go.countDown();for(var call:calls)call.get(10,TimeUnit.SECONDS);customer(1);assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_settlement",Long.class));assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_order_log WHERE action='COMPLETION_CONFIRMED'",Long.class));}finally{pool.shutdownNow();}}
}
