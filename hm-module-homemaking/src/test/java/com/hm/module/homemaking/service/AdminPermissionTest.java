package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.homemaking.controller.admin.*;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.*;
import com.hm.module.system.api.permission.PermissionApi;
import com.hm.module.system.service.permission.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Calls Spring-proxied controllers: verifies actual method security AND JDBC data predicates. */
@SpringJUnitConfig(AdminPermissionTest.Config.class)
class AdminPermissionTest {
    static final Map<Long,Set<String>> rolesByUser=new HashMap<>();
    static boolean grantMenus;
    @Configuration @EnableMethodSecurity @EnableAspectJAutoProxy
    @Import({BusinessIsolationTest.Config.class, HomemakingAdminAccess.class, AdminScopeAspect.class,
        StaffAccessService.class, HomemakingAdminController.class, HomemakingStaffController.class, HomemakingWorkerController.class,HomemakingPaymentController.class,HomemakingOrderChangeController.class})
    static class Config {
        @Bean PermissionApi permissionApi(){return mock(PermissionApi.class, call -> {
            if(call.getMethod().getName().equals("hasAnyPermissions"))return grantMenus;
            if(call.getMethod().getName().equals("hasAnyRoles")) {
                var codes=rolesByUser.getOrDefault(call.getArgument(0),Set.of());
                return Arrays.stream(call.getArguments()).skip(1).anyMatch(codes::contains);
            }
            return null;
        });}
        @Bean RoleService roles(){return mock(RoleService.class);}
        @Bean PermissionService permissions(){return mock(PermissionService.class);}
        @Bean BrandingService branding(){return mock(BrandingService.class);}
    }
    @Autowired JdbcTemplate jdbc; @Autowired DataSource dataSource;
    @Autowired HomemakingAdminController admin; @Autowired HomemakingStaffController staff;
    @Autowired HomemakingWorkerController worker; @Autowired HomemakingAdminAccess access;
    @Autowired HomemakingPaymentController payment;
    @Autowired HomemakingOrderChangeController orderChanges;
    @Autowired HmRepository repo; @Autowired OrderService orders;
    @Autowired PermissionService permissions; @Autowired RoleService roles;
    long orderId;
    @BeforeEach void seed() {
        jdbc.execute("DROP ALL OBJECTS"); rolesByUser.clear(); grantMenus=true; reset(permissions,roles);
        var scripts=new ResourceDatabasePopulator(new FileSystemResource("../sql/mysql/hm-homemaking.sql"),new FileSystemResource("../sql/mysql/upgrades/V002__operations_and_portal.sql"));
        scripts.setSqlScriptEncoding("UTF-8");scripts.execute(dataSource);
        BusinessTestSchema.payment(dataSource);
        jdbc.execute("CREATE TABLE system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(40),nickname VARCHAR(40),deleted BOOLEAN DEFAULT FALSE,status INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE system_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(40),deleted BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE system_user_role(user_id BIGINT,role_id BIGINT,tenant_id BIGINT,deleted BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE system_role_menu(role_id BIGINT,menu_id BIGINT,tenant_id BIGINT,creator VARCHAR(40),updater VARCHAR(40),deleted BOOLEAN)");
        jdbc.execute("CREATE TABLE system_menu(id BIGINT PRIMARY KEY,name VARCHAR(100),permission VARCHAR(100),type INT,sort INT,parent_id BIGINT,path VARCHAR(100),icon VARCHAR(50),component VARCHAR(100),component_name VARCHAR(100),status INT,visible BOOLEAN,keep_alive BOOLEAN,always_show BOOLEAN,creator VARCHAR(40),updater VARCHAR(40),deleted BOOLEAN)");
        for(long id=900000;id<=900006;id++)jdbc.update("INSERT INTO system_menu(id,status,deleted) VALUES(?,0,FALSE)",id);
        var upgrade=new ResourceDatabasePopulator(new FileSystemResource("../sql/mysql/upgrades/V004__admin_permissions.sql"));upgrade.setSqlScriptEncoding("UTF-8");upgrade.execute(dataSource);
        jdbc.update("INSERT INTO hm_customer(id,nickname) VALUES(1,'Customer')");
        jdbc.update("INSERT INTO hm_customer_tenant(tenant_id,customer_id) VALUES(1,1)");
        jdbc.update("INSERT INTO hm_store(id,tenant_id,name) VALUES(1,1,'Allowed'),(2,1,'Private'),(3,2,'Foreign')");
        jdbc.update("INSERT INTO hm_worker(id,tenant_id,store_id,name) VALUES(1,1,1,'A'),(2,1,2,'B')");
        jdbc.update("INSERT INTO hm_service(id,tenant_id,store_id,name,description,price_cents,duration_minutes) VALUES(1,1,1,'Clean','',10000,60),(2,1,2,'Clean','',10000,60)");
        jdbc.update("INSERT INTO hm_customer_address(id,tenant_id,customer_id,contact_name,phone,address) VALUES(1,1,1,'C','13800000001','Private address')");
        jdbc.update("INSERT INTO hm_worker_skill VALUES(1,1,1)");jdbc.update("INSERT INTO hm_worker_area VALUES(1,1,'*')");
        var start=LocalDate.now().plusDays(2).atTime(9,0);
        jdbc.update("INSERT INTO hm_worker_schedule(tenant_id,worker_id,starts_at,ends_at) VALUES(1,1,?,?)",start.minusHours(1),start.plusHours(8));
        login(1,1,1); orderId=orders.book(new OrderService.Book(1L,1L,start,1L,"permission-order"));
        jdbc.update("INSERT INTO system_users(id,tenant_id,username,nickname) VALUES(10,1,'staff','Staff'),(11,1,'target','Target'),(20,2,'foreign','Foreign')");
        jdbc.update("INSERT INTO hm_worker_account(tenant_id,worker_id,user_id) VALUES(1,1,10)");
    }
    @AfterEach void clear(){assertNull(AdminScope.current(),"request scope must always be cleared");SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void login(long tenant,long id,int type){TenantContextHolder.setTenantId(tenant);TenantContextHolder.setIgnore(false);var user=new LoginUser();user.setId(id);user.setTenantId(tenant);user.setUserType(type);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
    void role(String code){login(1,10,2);rolesByUser.put(10L,Set.of("hm_"+code.toLowerCase(Locale.ROOT)));jdbc.update("INSERT INTO hm_staff_scope(tenant_id,user_id,template_code,updated_by) VALUES(1,10,?,10) ON DUPLICATE KEY UPDATE template_code=VALUES(template_code)",code);jdbc.update("DELETE FROM hm_staff_store WHERE user_id=10");if(RoleTemplates.get(code).range().equals("STORES"))jdbc.update("INSERT INTO hm_staff_store VALUES(1,10,1)");}
    @ParameterizedTest @ValueSource(strings={"PLATFORM","OWNER","MANAGER","DISPATCHER","SUPPORT","FINANCE","WORKER"})
    void roleCeilingIsEnforcedEvenWithOverGrantedSystemMenus(String code){
        role(code);var expected=RoleTemplates.get(code).permissions();
        for(String p:RoleTemplates.ALL)assertEquals(expected.contains(p),access.allowed(p),code+" "+p);
        if(expected.contains("homemaking:orders:read"))assertDoesNotThrow(()->admin.order(orderId));else assertThrows(AccessDeniedException.class,()->admin.order(orderId));
        if(!expected.contains("homemaking:orders:dispatch"))assertThrows(AccessDeniedException.class,()->admin.assign(orderId,new HomemakingAdminController.Assign(1L)));
        if(!code.equals("PLATFORM"))assertThrows(AccessDeniedException.class,()->admin.plans());
    }
    @Test void listAndCountUseTheSameStoreScope(){role("MANAGER");var page=(Map<?,?>)admin.catalog("stores",1,20).getData();assertEquals(1L,page.get("total"));assertEquals(1,((List<?>)page.get("list")).size());assertThrows(Exception.class,()->admin.serviceSettings(2));assertThrows(Exception.class,()->admin.calendar(2,LocalDate.now(),LocalDate.now()));}
    @Test void supportCanAmendAddressButCannotForgeAManualPrice(){
        role("SUPPORT");long version=HmRepository.number(repo.require("hm_order",orderId,false),"version");
        var address=new OrderChangeService.Contact("Customer","13800000001","Updated address","");
        assertThrows(AccessDeniedException.class,()->orderChanges.preview(orderId,new OrderChangeService.Request(version,address,9000,"Manual price","support-forgery")));
        assertDoesNotThrow(()->orderChanges.create(orderId,new OrderChangeService.Request(version,address,null,"Address confirmed","support-address",10000)));
    }
    @Test void orderAmendmentIsDeniedToFinanceAndDispatcherEvenWithMenuOvergrant(){
        var request=new OrderChangeService.Request(0,null,11000,"Reprice","unauthorized-price");
        for(String template:List.of("FINANCE","DISPATCHER","WORKER")){role(template);assertThrows(AccessDeniedException.class,()->orderChanges.create(orderId,request));}
        role("MANAGER");long version=HmRepository.number(repo.require("hm_order",orderId,false),"version");assertDoesNotThrow(()->orderChanges.create(orderId,new OrderChangeService.Request(version,null,11000,"Customer confirmed","manager-price")));
    }
    @Test void removingStoreGrantImmediatelyHidesRowsAndIds(){role("SUPPORT");assertNotNull(admin.order(orderId));jdbc.update("DELETE FROM hm_staff_store WHERE user_id=10");assertThrows(Exception.class,()->admin.order(orderId));assertEquals(0L,((Map<?,?>)admin.orders(1,20).getData()).get("total"));assertTrue(((List<?>)admin.customers().getData()).isEmpty());}
    @Test void cannotEditServiceIntoAnUnauthorizedStore(){role("MANAGER");var req=new CatalogService.Save(1L,"Changed",null,null,null,2L,null,null,"Clean","",10000,60,null,"ACTIVE",0L);assertThrows(Exception.class,()->admin.save("services",req));assertEquals(1L,jdbc.queryForObject("SELECT store_id FROM hm_service WHERE id=1",Long.class));}
    @Test void headquartersFinanceCannotReadAnotherTenantsStatementsOrQuotas(){role("FINANCE");assertThrows(AccessDeniedException.class,()->admin.statements(2));assertThrows(AccessDeniedException.class,()->admin.quota(2L));}
    @Test void unassignedOrLegacyBroadPermissionFailsClosed(){login(1,10,2);rolesByUser.put(10L,Set.of("legacy_admin"));assertThrows(AccessDeniedException.class,()->admin.orders(1,20));assertThrows(AccessDeniedException.class,()->staff.me());}
    @Test void systemMenuRevocationIsRespected(){role("OWNER");grantMenus=false;assertThrows(AccessDeniedException.class,()->admin.orders(1,20));}
    @Test void tenantVisitHeaderCannotElevateOrdinaryStaff(){role("OWNER");TenantContextHolder.setTenantId(2L);assertThrows(AccessDeniedException.class,()->admin.orders(1,20));}
    @Test void platformCanVisitButLocalSuperAdminCannotBecomePlatform(){login(2,20,2);rolesByUser.put(20L,Set.of("super_admin"));assertFalse(access.allowed("homemaking:platform:manage"));assertTrue(access.allowed("homemaking:orders:read"));role("PLATFORM");TenantContextHolder.setTenantId(2L);assertDoesNotThrow(()->admin.orders(1,20));}
    @Test void workerOnlySeesOwnAssignedTasks(){role("WORKER");jdbc.update("UPDATE hm_order SET status='PAID' WHERE id=?",orderId);assertEquals(1,((List<?>)((Map<?,?>)worker.tasks(LocalDate.now(),LocalDate.now().plusDays(4)).getData()).get("tasks")).size());jdbc.update("UPDATE hm_order SET worker_id=2 WHERE id=?",orderId);assertThrows(Exception.class,()->worker.evidence(orderId));}
    @Test void ownerCannotGrantPlatformOrForeignUserOrEditSelf(){role("OWNER");assertThrows(AccessDeniedException.class,()->staff.grant(new StaffAccessService.Grant(11,"PLATFORM",Set.of())));assertThrows(Exception.class,()->staff.grant(new StaffAccessService.Grant(20,"SUPPORT",Set.of(1L))));assertThrows(AccessDeniedException.class,()->staff.grant(new StaffAccessService.Grant(10,"FINANCE",Set.of(1L))));verifyNoInteractions(permissions);}
    @Test void managerCannotAssignAnyRole(){role("MANAGER");assertThrows(AccessDeniedException.class,()->staff.grant(new StaffAccessService.Grant(11,"OWNER",Set.of())));verifyNoInteractions(permissions);}
    @Test void successfulGrantReplacesOldHomemakingRoleAndAuditsScope(){
        role("OWNER");
        var old=new com.hm.module.system.dal.dataobject.permission.RoleDO().setId(90L).setCode("hm_owner").setStatus(0);
        var other=new com.hm.module.system.dal.dataobject.permission.RoleDO().setId(91L).setCode("auditor").setStatus(0);
        var finance=new com.hm.module.system.dal.dataobject.permission.RoleDO().setId(92L).setCode("hm_finance").setStatus(0);
        when(permissions.getUserRoleIdListByUserId(11L)).thenReturn(Set.of(90L,91L));when(roles.getRoleList(Set.of(90L,91L))).thenReturn(List.of(old,other));when(roles.getRoleList()).thenReturn(List.of(old,other,finance));
        staff.grant(new StaffAccessService.Grant(11,"FINANCE",Set.of(1L)));
        verify(permissions).assignUserRole(11L,Set.of(91L,92L));
        verify(permissions).assignRoleMenu(eq(92L),argThat(ids->ids.contains(900005L)&&!ids.contains(900007L)));
        assertEquals("FINANCE",jdbc.queryForObject("SELECT template_code FROM hm_staff_scope WHERE user_id=11",String.class));
        assertEquals(List.of(1L),jdbc.queryForList("SELECT store_id FROM hm_staff_store WHERE user_id=11",Long.class));
        assertEquals(10L,jdbc.queryForObject("SELECT operator_id FROM hm_staff_access_log WHERE user_id=11",Long.class));
    }
    @Test void selfAndSuperAdminAreNotPresentedAsEditable(){
        role("OWNER");jdbc.update("INSERT INTO system_role(id,tenant_id,code) VALUES(99,1,'super_admin')");jdbc.update("INSERT INTO system_user_role(user_id,role_id,tenant_id) VALUES(11,99,1)");
        var rows=(List<Map<String,Object>>)staff.users().getData();assertEquals(2,rows.size());assertTrue(rows.stream().noneMatch(r->Boolean.TRUE.equals(r.get("editable"))));
    }
    @Test void noStoreAssignmentAndForeignStoreAreRejected(){role("OWNER");when(permissions.getUserRoleIdListByUserId(11L)).thenReturn(Set.of());when(roles.getRoleList(Set.of())).thenReturn(List.of());assertThrows(Exception.class,()->staff.grant(new StaffAccessService.Grant(11,"MANAGER",Set.of())));assertThrows(Exception.class,()->staff.grant(new StaffAccessService.Grant(11,"MANAGER",Set.of(3L))));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_staff_scope WHERE user_id=11",Integer.class));}
    @Test void allAdminMappingsDeclareSecurityAndNoBroadManageRemains(){
        for(var controller:List.of(HomemakingAdminController.class,HomemakingStaffController.class,HomemakingPaymentController.class))for(var method:controller.getDeclaredMethods()){
            if(Arrays.stream(method.getAnnotations()).noneMatch(a->a.annotationType().getSimpleName().endsWith("Mapping")))continue;
            var annotation=method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            assertNotNull(annotation,method.getName());assertFalse(annotation.value().contains("homemaking:manage"));
        }
    }
    @Test void onlyAuthorizedOperationsCanReceiveAndOnlyFinanceCanReverse(){
        role("SUPPORT");var request=new PaymentLedgerService.Receipt("CASH",10000,LocalDateTime.now().withNano(0),"Cash checked","controller-receipt");
        assertThrows(AccessDeniedException.class,()->payment.receive(orderId,request));
        role("MANAGER");long entry=((Number)payment.receive(orderId,request).getData()).longValue();
        var reversal=new PaymentLedgerService.Reversal(entry,LocalDateTime.now().withNano(0),"Mistaken entry","controller-reversal");
        assertThrows(AccessDeniedException.class,()->payment.reverse(orderId,reversal));
        role("FINANCE");assertDoesNotThrow(()->payment.reverse(orderId,reversal));
    }
    @Test void financeCannotChooseForeignTenantInLedgerQuery(){role("FINANCE");assertThrows(AccessDeniedException.class,()->payment.report(LocalDate.now(),LocalDate.now(),1,20,2L));assertEquals(1L,TenantContextHolder.getTenantId());}
}
