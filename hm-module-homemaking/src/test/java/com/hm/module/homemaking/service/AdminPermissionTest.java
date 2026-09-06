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
        PlatformAccessService.class, com.hm.module.system.controller.admin.permission.PlatformAccessController.class,
        com.hm.module.system.controller.admin.tenant.TenantController.class,
        StaffAccessService.class, HomemakingAdminController.class, HomemakingStaffController.class, HomemakingWorkerController.class,HomemakingPaymentController.class,HomemakingOrderChangeController.class,HomemakingTimeOffController.class})
    static class Config {
        @Bean(name="ss") com.hm.framework.security.core.service.SecurityFrameworkService security(PermissionApi api){return new com.hm.framework.security.core.service.SecurityFrameworkServiceImpl(api);}
        @Bean com.hm.module.system.service.tenant.TenantService tenantService(){return mock(com.hm.module.system.service.tenant.TenantService.class);}
        @Bean PermissionApi permissionApi(PlatformAccessService platform){return mock(PermissionApi.class, call -> {
            if(call.getMethod().getName().equals("isPlatformUser"))return platform.isOperator(call.getArgument(0),call.getArgument(1));
            if(call.getMethod().getName().equals("canVisitTenant"))return platform.canVisit(call.getArgument(0),call.getArgument(1),call.getArgument(2));
            if(call.getMethod().getName().equals("recordPlatformVisit")){platform.visitLog(call.getArgument(0),call.getArgument(1),call.getArgument(2),call.getArgument(3),call.getArgument(4));return null;}
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
    @Autowired HomemakingTimeOffController timeOff;
    @Autowired HomemakingOrderChangeController orderChanges;
    @Autowired HmRepository repo; @Autowired OrderService orders;
    @Autowired PermissionService permissions; @Autowired RoleService roles;
    @Autowired PlatformAccessService platform;
    @Autowired com.hm.module.system.controller.admin.permission.PlatformAccessController platformController;
    @Autowired com.hm.module.system.controller.admin.tenant.TenantController tenants;
    @Autowired com.hm.framework.security.core.service.SecurityFrameworkService security;
    long orderId;
    @BeforeEach void seed() {
        jdbc.execute("DROP ALL OBJECTS"); rolesByUser.clear(); grantMenus=true; reset(permissions,roles);
        var scripts=new ResourceDatabasePopulator(new FileSystemResource("../sql/mysql/hm-homemaking.sql"),new FileSystemResource("../sql/mysql/upgrades/V002__operations_and_portal.sql"));
        scripts.setSqlScriptEncoding("UTF-8");scripts.execute(dataSource);
        BusinessTestSchema.payment(dataSource);
        jdbc.execute("CREATE TABLE system_users(id BIGINT PRIMARY KEY,tenant_id BIGINT,username VARCHAR(40),nickname VARCHAR(40),deleted BOOLEAN DEFAULT FALSE,status INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE system_role(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(40),status INT DEFAULT 0,deleted BOOLEAN DEFAULT FALSE)");
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
        jdbc.execute("CREATE TABLE system_tenant(id BIGINT PRIMARY KEY,name VARCHAR(100),status INT DEFAULT 0,deleted BOOLEAN DEFAULT FALSE,expire_time TIMESTAMP DEFAULT '2099-01-01 00:00:00',package_id BIGINT DEFAULT 0)");
        jdbc.update("INSERT INTO system_tenant(id,name) VALUES(1,'直营租户'),(2,'加盟租户'),(3,'已停用')");
        jdbc.update("UPDATE system_tenant SET status=1 WHERE id=3");
        jdbc.execute("CREATE TABLE system_tenant_package(id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(100),status INT,remark VARCHAR(256),menu_ids VARCHAR(4096),creator VARCHAR(40),updater VARCHAR(40),deleted BOOLEAN)");
        jdbc.update("INSERT INTO system_users(id,tenant_id,username,nickname) VALUES(1,1,'bootstrap','Original platform')");
        jdbc.update("INSERT INTO system_role(id,tenant_id,code) VALUES(1,1,'super_admin')");jdbc.update("INSERT INTO system_user_role VALUES(1,1,1,FALSE)");
        var identity=new ResourceDatabasePopulator(new FileSystemResource("../sql/mysql/upgrades/V007__platform_identity.sql"));identity.setSqlScriptEncoding("UTF-8");identity.execute(dataSource);
    }
    @AfterEach void clear(){assertNull(AdminScope.current(),"request scope must always be cleared");SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void login(long tenant,long id,int type){TenantContextHolder.setTenantId(tenant);TenantContextHolder.setIgnore(false);var user=new LoginUser();user.setId(id);user.setTenantId(tenant);user.setUserType(type);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));}
    void role(String code){login(1,10,2);rolesByUser.put(10L,Set.of("hm_"+code.toLowerCase(Locale.ROOT)));jdbc.update("INSERT INTO hm_staff_scope(tenant_id,user_id,template_code,updated_by) VALUES(1,10,?,10) ON DUPLICATE KEY UPDATE template_code=VALUES(template_code)",code);jdbc.update("DELETE FROM hm_staff_store WHERE user_id=10");if(RoleTemplates.get(code).range().equals("STORES"))jdbc.update("INSERT INTO hm_staff_store VALUES(1,10,1)");if(code.equals("PLATFORM"))jdbc.update("INSERT INTO system_platform_operator(user_id,account_tenant_id,reason) VALUES(10,1,'Explicit test grant')");}
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
    @Test void reviewReplyAndModerationHaveSeparateRoleCeilings(){
        jdbc.update("UPDATE hm_order SET status='COMPLETED',fulfillment_status='COMPLETED' WHERE id=?",orderId);
        long review=repo.insert("INSERT INTO hm_review(tenant_id,customer_id,order_id,rating,content,service_rating,worker_rating,status) VALUES(1,1,?,5,'Excellent',5,5,'PUBLISHED')",orderId);
        role("SUPPORT");assertDoesNotThrow(()->admin.reply(review,new ReviewService.Reply("Thank you")));assertThrows(AccessDeniedException.class,()->admin.moderation(review,new ReviewService.Moderation(true,true)));
        role("MANAGER");assertDoesNotThrow(()->admin.moderation(review,new ReviewService.Moderation(true,true)));
        role("FINANCE");assertThrows(AccessDeniedException.class,()->admin.review(review));assertThrows(AccessDeniedException.class,()->admin.reply(review,new ReviewService.Reply("Forbidden")));
    }
    @Test void removingStoreGrantImmediatelyHidesRowsAndIds(){role("SUPPORT");assertNotNull(admin.order(orderId));jdbc.update("DELETE FROM hm_staff_store WHERE user_id=10");assertThrows(Exception.class,()->admin.order(orderId));assertEquals(0L,((Map<?,?>)admin.orders(1,20).getData()).get("total"));assertTrue(((List<?>)admin.customers().getData()).isEmpty());}
    @Test void cannotEditServiceIntoAnUnauthorizedStore(){role("MANAGER");var req=new CatalogService.Save(1L,"Changed",null,null,null,2L,null,null,"Clean","",10000,60,null,"ACTIVE",0L);assertThrows(Exception.class,()->admin.save("services",req));assertEquals(1L,jdbc.queryForObject("SELECT store_id FROM hm_service WHERE id=1",Long.class));}
    @Test void headquartersFinanceCannotReadAnotherTenantsStatementsOrQuotas(){role("FINANCE");assertThrows(AccessDeniedException.class,()->admin.statements(2));assertThrows(AccessDeniedException.class,()->admin.quota(2L));}
    @Test void unassignedOrLegacyBroadPermissionFailsClosed(){login(1,10,2);rolesByUser.put(10L,Set.of("legacy_admin"));assertThrows(AccessDeniedException.class,()->admin.orders(1,20));assertThrows(AccessDeniedException.class,()->staff.me());}
    @Test void systemMenuRevocationIsRespected(){role("OWNER");grantMenus=false;assertThrows(AccessDeniedException.class,()->admin.orders(1,20));}
    @Test void tenantVisitHeaderCannotElevateOrdinaryStaff(){role("OWNER");TenantContextHolder.setTenantId(2L);assertThrows(AccessDeniedException.class,()->admin.orders(1,20));}
    @Test void platformCanVisitButLocalSuperAdminCannotBecomePlatform(){login(2,20,2);rolesByUser.put(20L,Set.of("super_admin"));assertFalse(access.allowed("homemaking:platform:manage"));assertTrue(access.allowed("homemaking:orders:read"));role("PLATFORM");TenantContextHolder.setTenantId(2L);assertDoesNotThrow(()->admin.orders(1,20));}
    @Test void workerOnlySeesOwnAssignedTasks(){role("WORKER");jdbc.update("UPDATE hm_order SET status='PAID' WHERE id=?",orderId);assertEquals(1,((List<?>)((Map<?,?>)worker.tasks(LocalDate.now(),LocalDate.now().plusDays(4)).getData()).get("tasks")).size());jdbc.update("UPDATE hm_order SET worker_id=2 WHERE id=?",orderId);assertThrows(Exception.class,()->worker.evidence(orderId));}
    @Nested class WorkerWorkbenchPermissions {
        LocalDate date(){return LocalDate.now().plusDays(2);}
        WorkerTimeOffService.Request request(){return new WorkerTimeOffService.Request(date().atTime(12,0),date().atTime(13,0),"REST","Personal rest",UUID.randomUUID().toString());}
        long apply(){role("WORKER");return ((Number)worker.request(request()).getData()).longValue();}
        @Test void workerCanUseOwnWorkbenchButCannotApproveEvenWithOverGrantedMenus(){
            long id=apply();assertDoesNotThrow(()->worker.calendar(date(),date()));assertDoesNotThrow(()->worker.income(date(),date(),"ENTRIES",1,20));
            assertThrows(AccessDeniedException.class,()->timeOff.review(1,id,new WorkerTimeOffService.Decision(0,true,"Self approval")));
            assertThrows(AccessDeniedException.class,()->admin.leave(1,new ScheduleService.Interval(date().atTime(14,0),date().atTime(15,0),"Bypass approval")));
            assertThrows(AccessDeniedException.class,()->admin.removeLeave(1,id));
        }
        @Test void dispatcherAndSupportMayReadButCannotApproveOrUseLegacyLeaveWrites(){
            long id=apply();for(String code:List.of("DISPATCHER","SUPPORT")){role(code);assertDoesNotThrow(()->timeOff.list(1,date(),date()));assertThrows(AccessDeniedException.class,()->timeOff.review(1,id,new WorkerTimeOffService.Decision(0,true,"Forbidden")));assertThrows(AccessDeniedException.class,()->admin.leave(1,new ScheduleService.Interval(date().atTime(14,0),date().atTime(15,0),"Forbidden")));assertThrows(AccessDeniedException.class,()->admin.removeLeave(1,id));}
        }
        @Test void managerApprovesOnlyGrantedStoreAndCannotReadOwnWorkerIncome(){
            long id=apply();role("MANAGER");assertThrows(Exception.class,()->timeOff.list(2,date(),date()));assertThrows(Exception.class,()->timeOff.review(2,id,new WorkerTimeOffService.Decision(0,true,"Other store")));
            assertDoesNotThrow(()->timeOff.review(1,id,new WorkerTimeOffService.Decision(0,true,"Approved")));assertThrows(AccessDeniedException.class,()->worker.income(date(),date(),"ENTRIES",1,20));
        }
        @Test void ordinaryTenantOwnerMayReviewButForgedVisitCannotReadWorkbench(){long id=apply();role("OWNER");assertDoesNotThrow(()->timeOff.review(1,id,new WorkerTimeOffService.Decision(0,true,"Tenant review")));role("WORKER");TenantContextHolder.setTenantId(2L);assertThrows(AccessDeniedException.class,()->worker.calendar(date(),date()));assertThrows(AccessDeniedException.class,()->worker.income(date(),date(),"PAID",1,20));}
        @Test void menuMigrationAddsReviewAndIncomeWithoutElevatingDispatcher() throws Exception {
            jdbc.update("INSERT INTO system_role(id,tenant_id,code) VALUES(501,1,'hm_worker'),(502,1,'hm_manager'),(503,1,'hm_dispatcher')");
            String dml="INSERT INTO system_menu"+java.nio.file.Files.readString(java.nio.file.Path.of("../sql/mysql/upgrades/V008__worker_workbench.sql")).split("INSERT INTO system_menu",2)[1];
            var populator=new ResourceDatabasePopulator(new org.springframework.core.io.ByteArrayResource(dml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));populator.setSqlScriptEncoding("UTF-8");populator.execute(dataSource);
            assertEquals(List.of(901120L,901121L),jdbc.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id=501 ORDER BY menu_id",Long.class));
            assertEquals(List.of(901122L),jdbc.queryForList("SELECT menu_id FROM system_role_menu WHERE role_id=502",Long.class));assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM system_role_menu WHERE role_id=503",Long.class));
            var ids=new com.fasterxml.jackson.databind.ObjectMapper().readTree(jdbc.queryForObject("SELECT menu_ids FROM system_tenant_package WHERE name='HM 直营业务'",String.class));
            for(long id:List.of(901120L,901121L,901122L))assertTrue(java.util.stream.StreamSupport.stream(ids.spliterator(),false).anyMatch(node->node.asLong()==id));
            assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM hm_schema_upgrade WHERE version='V008'",Long.class));
        }
    }
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
        var rows=(List<Map<String,Object>>)staff.users().getData();assertEquals(3,rows.size());assertTrue(rows.stream().noneMatch(r->Boolean.TRUE.equals(r.get("editable"))));
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

    @Nested class PlatformIdentity {
        void root(){login(1,1,2);}
        void grantForeign(){root();platformController.grant(new PlatformAccessService.Grant(20,2,true,0,"平台运维授权"),new org.springframework.mock.web.MockHttpServletRequest());}
        com.hm.framework.tenant.core.web.TenantVisitContextInterceptor interceptor(){return new com.hm.framework.tenant.core.web.TenantVisitContextInterceptor(new com.hm.framework.tenant.config.TenantProperties(),security);}
        @Test void upgradePreservesOnlyPreviouslyAuthorizedAccountsAndMakesHeadquartersOrdinary() throws Exception {
            assertTrue(platform.isOperator(1L,1L));assertFalse(platform.isOperator(10L,1L));assertFalse(platform.isOperator(20L,2L));
            assertTrue(jdbc.queryForObject("SELECT package_id FROM system_tenant WHERE id=1",Long.class)>0);
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_platform_access_log WHERE action='MIGRATE'",Integer.class));
            var menus=new com.fasterxml.jackson.databind.ObjectMapper().readTree(jdbc.queryForObject("SELECT menu_ids FROM system_tenant_package WHERE name='HM 直营业务'",String.class));
            assertTrue(menus.isArray());assertTrue(menus.size()>20);assertFalse(menus.toString().contains("900008"));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_schema_upgrade WHERE version='V007'",Integer.class));
        }
        @Test void tenantOneSuperAdminIsOnlyBusinessOwnerWithoutExplicitPlatformGrant(){
            login(1,10,2);rolesByUser.put(10L,Set.of("super_admin"));
            assertEquals("OWNER",access.template().code());assertFalse(access.platform());
            assertDoesNotThrow(()->admin.order(orderId));assertThrows(AccessDeniedException.class,()->admin.plans());
            assertThrows(AccessDeniedException.class,()->tenants.getTenant(2L));assertThrows(AccessDeniedException.class,()->tenants.getTenantSimpleList());
            assertFalse(security.hasPermission("system:tenant:visit"));assertFalse(security.hasPermission("system:menu:update"));
            assertThrows(AccessDeniedException.class,()->admin.quota(2L));assertThrows(AccessDeniedException.class,()->admin.statements(2));
        }
        @Test void oldPlatformRoleAndOvergrantedMenusCannotCreatePlatformIdentity(){
            login(2,20,2);rolesByUser.put(20L,Set.of("super_admin","hm_platform"));
            assertFalse(access.platform());assertFalse(security.canVisitTenant(1L));assertFalse(security.isPlatform());
            assertThrows(AccessDeniedException.class,()->platformController.operators());
            assertThrows(AccessDeniedException.class,()->platformController.grant(new PlatformAccessService.Grant(10,1,true,0,"forge"),new org.springframework.mock.web.MockHttpServletRequest()));
        }
        @Test void platformIdentityCanLiveInAnyTenantAndManageOtherTenants(){
            grantForeign();login(2,20,2);assertTrue(access.platform());assertTrue(security.isPlatform());
            assertDoesNotThrow(()->admin.quota(1L));assertDoesNotThrow(()->admin.plans());assertDoesNotThrow(()->tenants.getTenant(1L));
            assertDoesNotThrow(()->admin.commissionRule(1,new SettlementService.Rule(500,5000,30,true,0)));
            assertEquals(500,jdbc.queryForObject("SELECT platform_bps FROM hm_commission_rule WHERE tenant_id=1",Integer.class));
            TenantContextHolder.setTenantId(1L);assertDoesNotThrow(()->admin.order(orderId));
            assertEquals(1L,access.current().get("businessTenantId"));assertEquals(2L,access.current().get("accountTenantId"));
        }
        @Test void realVisitInterceptorValidatesTargetAndRestoresContextAndAudits(){
            grantForeign();login(2,20,2);var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/admin-api/homemaking/orders");request.addHeader("visit-tenant-id","1");var response=new org.springframework.mock.web.MockHttpServletResponse();var filter=interceptor();
            assertTrue(filter.preHandle(request,response,new Object()));assertEquals(1L,repo.tenant());assertDoesNotThrow(()->admin.order(orderId));
            filter.afterCompletion(request,response,null,null);assertEquals(2L,repo.tenant());assertNull(com.hm.framework.security.core.util.SecurityFrameworkUtils.getLoginUser().getVisitTenantId());
            var audit=jdbc.queryForMap("SELECT * FROM system_platform_access_log WHERE action='VISIT'");assertEquals(20L,audit.get("operator_id"));assertEquals(2L,audit.get("account_tenant_id"));assertEquals(1L,audit.get("target_tenant_id"));
            for(String invalid:List.of("3","999","-1")){var r=new org.springframework.mock.web.MockHttpServletRequest();r.addHeader("visit-tenant-id",invalid);assertThrows(Exception.class,()->filter.preHandle(r,response,new Object()));assertEquals(2L,repo.tenant());}
        }
        @Test void tenantRolesCannotUseVisitHeaderEvenWithAllMenus(){
            for(String code:List.of("OWNER","MANAGER","FINANCE","WORKER")){role(code);var request=new org.springframework.mock.web.MockHttpServletRequest();request.addHeader("visit-tenant-id","2");assertThrows(Exception.class,()->interceptor().preHandle(request,new org.springframework.mock.web.MockHttpServletResponse(),null));assertEquals(1L,repo.tenant());}
        }
        @Test void revokeIsImmediateAndKeepsBusinessAuthorityWhenSeparatelyAssigned(){
            grantForeign();root();platformController.grant(new PlatformAccessService.Grant(20,2,false,1,"职责变更"),new org.springframework.mock.web.MockHttpServletRequest());
            login(2,20,2);rolesByUser.put(20L,Set.of("super_admin"));assertFalse(access.platform());assertTrue(access.allowed("homemaking:orders:read"));assertFalse(security.canVisitTenant(1L));
            assertThrows(AccessDeniedException.class,()->admin.plans());TenantContextHolder.setTenantId(1L);assertThrows(AccessDeniedException.class,()->admin.order(orderId));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_platform_access_log WHERE action='REVOKE' AND operator_id=1 AND target_user_id=20 AND before_value='true' AND after_value='false'",Integer.class));
        }
        @Test void accountTenantClaimAndCustomerTokenCannotImpersonatePlatformUser(){
            root();login(2,1,2);assertFalse(access.platform());assertFalse(security.isPlatform());login(1,1,1);assertFalse(access.platform());assertFalse(security.isPlatform());
        }
        @Test void platformGrantRejectsSelfWrongTenantDisabledUserAndStaleVersion(){
            root();var request=new org.springframework.mock.web.MockHttpServletRequest();
            assertThrows(AccessDeniedException.class,()->platformController.grant(new PlatformAccessService.Grant(1,1,false,1,"self"),request));
            assertThrows(AccessDeniedException.class,()->platformController.grant(new PlatformAccessService.Grant(20,1,true,0,"wrong partition"),request));
            jdbc.update("UPDATE system_users SET status=1 WHERE id=20");assertThrows(AccessDeniedException.class,()->platformController.grant(new PlatformAccessService.Grant(20,2,true,0,"disabled"),request));jdbc.update("UPDATE system_users SET status=0 WHERE id=20");
            grantForeign();assertThrows(IllegalArgumentException.class,()->platformController.grant(new PlatformAccessService.Grant(20,2,false,0,"stale"),request));assertTrue(platform.isOperator(20L,2L));
        }
        @Test void disablingOrDeletingAccountImmediatelyInvalidatesPlatformIdentity(){grantForeign();jdbc.update("UPDATE system_users SET status=1 WHERE id=20");assertFalse(platform.isOperator(20L,2L));jdbc.update("UPDATE system_users SET status=0,deleted=TRUE WHERE id=20");assertFalse(platform.isOperator(20L,2L));}
        @Test void protectedPlatformAccountsCannotBeTakenOverThroughTenantUserManagement(){
            grantForeign();login(2,20,2);platform.guardUserMutation(20);login(1,10,2);rolesByUser.put(10L,Set.of("super_admin"));
            var users=new com.hm.module.system.service.user.AdminUserServiceImpl();org.springframework.test.util.ReflectionTestUtils.setField(users,"platformAccess",platform);
            assertThrows(AccessDeniedException.class,()->users.updateUserPassword(1L,"cannot-reset"));assertThrows(AccessDeniedException.class,()->users.updateUser(new com.hm.module.system.controller.admin.user.vo.user.UserSaveReqVO().setId(1L)));
            assertThrows(AccessDeniedException.class,()->users.updateUserStatus(1L,1));assertThrows(AccessDeniedException.class,()->users.deleteUserList(List.of(1L)));
            root();assertThrows(AccessDeniedException.class,()->users.deleteUser(1L));assertDoesNotThrow(()->platform.guardUserMutation(11));
        }
        @Test void platformTemplateCannotBeGrantedThroughOrdinaryStaffRoleFlow(){root();assertThrows(AccessDeniedException.class,()->staff.grant(new StaffAccessService.Grant(11,"PLATFORM",Set.of())));assertTrue(((List<RoleTemplates.Template>)staff.templates().getData()).stream().noneMatch(t->t.code().equals("PLATFORM")));}
        @Test void platformOnlyServiceCallsFailClosedWithoutControllerScope(){
            root();assertNull(AdminScope.current());assertThrows(AccessDeniedException.class,()->com.hm.module.homemaking.security.AdminScope.platformOnly());
            assertThrows(AccessDeniedException.class,()->com.hm.module.homemaking.security.AdminScope.tenant(2));
        }
        @Test void concurrentMutualRevocationsCannotRemoveAllPlatformOperators() throws Exception {
            grantForeign();var pool=java.util.concurrent.Executors.newFixedThreadPool(2);var ready=new java.util.concurrent.CountDownLatch(2);var start=new java.util.concurrent.CountDownLatch(1);
            try { var results=new ArrayList<java.util.concurrent.Future<Boolean>>();for(long actor:List.of(1L,20L))results.add(pool.submit(()->{login(actor==1?1:2,actor,2);ready.countDown();start.await();try{platform.grant(new PlatformAccessService.Grant(actor==1?20:1,actor==1?2:1,false,1,"并发撤销测试"),"127.0.0.1");return true;}catch(AccessDeniedException ex){return false;}finally{SecurityContextHolder.clearContext();TenantContextHolder.clear();}}));assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));start.countDown();int successes=0;for(var result:results)if(result.get(10,java.util.concurrent.TimeUnit.SECONDS))successes++;assertEquals(1,successes);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_platform_operator WHERE enabled=TRUE",Integer.class));}
            finally {start.countDown();pool.shutdownNow();}
        }
    }
}
