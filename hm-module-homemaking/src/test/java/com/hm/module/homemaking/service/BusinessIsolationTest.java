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
        @Bean PricingService pricing(HmRepository r,CustomerAccess c){return new PricingService(r,c,new com.fasterxml.jackson.databind.ObjectMapper());}
        @Bean OrderService orders(HmRepository r,CustomerAccess c,NotificationService n,PricingService p){return new OrderService(r,c,n,p);}
        @Bean CatalogService catalog(HmRepository r){return new CatalogService(r);}
    }
    @Autowired JdbcTemplate jdbc;@Autowired DataSource dataSource;@Autowired OrderService orders;@Autowired CatalogService catalog;@Autowired HmRepository repo;
    @BeforeEach void seed(){
        jdbc.execute("DROP ALL OBJECTS");
        var scripts=new ResourceDatabasePopulator(new FileSystemResource(Path.of("../sql/mysql/hm-homemaking.sql")));scripts.setSqlScriptEncoding("UTF-8");scripts.execute(dataSource);
        jdbc.update("INSERT INTO hm_customer(id,nickname) VALUES(1,'A'),(2,'B')");
        jdbc.update("INSERT INTO hm_customer_tenant(tenant_id,customer_id) VALUES(1,1),(1,2),(2,2)");
        jdbc.update("INSERT INTO hm_store(id,tenant_id,name) VALUES(1,1,'HQ'),(2,2,'Franchise')");
        jdbc.update("INSERT INTO hm_worker(id,tenant_id,store_id,name) VALUES(1,1,1,'Alice'),(2,2,2,'Bob')");
        jdbc.update("INSERT INTO hm_service(id,tenant_id,store_id,name,description,price_cents,duration_minutes) VALUES(1,1,1,'Clean','',10000,60),(2,2,2,'Clean','',20000,60)");
        jdbc.update("INSERT INTO hm_customer_address(id,tenant_id,customer_id,contact_name,phone,address) VALUES(1,1,1,'A','13800000001','Address A'),(2,1,2,'B','13800000002','Address B'),(3,2,2,'B','13800000002','Address C')");
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
}
