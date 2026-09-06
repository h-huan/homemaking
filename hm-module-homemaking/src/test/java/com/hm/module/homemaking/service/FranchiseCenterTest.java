package com.hm.module.homemaking.service;

import com.hm.framework.security.core.LoginUser;
import com.hm.framework.tenant.core.context.TenantContextHolder;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(BusinessIsolationTest.Config.class)
class FranchiseCenterTest {
    @Autowired JdbcTemplate jdbc;@Autowired DataSource dataSource;@Autowired FranchiseService franchises;
    @BeforeEach void seed(){
        var fixture=new BusinessIsolationTest();fixture.jdbc=jdbc;fixture.dataSource=dataSource;fixture.seed();
        jdbc.execute("CREATE TABLE system_tenant(id BIGINT PRIMARY KEY,name VARCHAR(100),contact_name VARCHAR(50),status INT,expire_time TIMESTAMP,deleted BOOLEAN)");
        jdbc.update("INSERT INTO system_tenant VALUES(1,'总部直营','总部',0,?,FALSE),(2,'华东合作商','李经理',0,?,FALSE)",LocalDateTime.now().plusYears(10),LocalDateTime.now().plusYears(3));
        platform();
    }
    @AfterEach void clear(){AdminScope.set(null);SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    void platform(){TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);var user=new LoginUser();user.setId(88L);user.setTenantId(1L);user.setUserType(2);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));AdminScope.set(new AdminScope(1,true,"TENANT",Set.of(),null));}
    FranchiseService.Profile profile(long version){return new FranchiseService.Profile(2,"FR-EAST-001","华东安心家政有限公司","91310000TEST", "李经理","13800138000","ops@example.com","上海市浦东新区","华东安心家政有限公司","示例银行上海分行","6222020000001234",List.of(new FranchiseService.Region("310000","上海市"),new FranchiseService.Region("330000","浙江省")),version);}
    FranchiseService.Contract contract(String no,LocalDate start,LocalDate end,long deposit,long version){return new FranchiseService.Contract(no,start.minusDays(10),start,end,deposit,"年度加盟合同",version);}
    long create(){return franchises.create(profile(0));}

    @Test void everyOperationRequiresExplicitPlatformScope(){AdminScope.set(new AdminScope(1,false,"TENANT",Set.of(),null));assertThrows(Exception.class,franchises::list);assertThrows(Exception.class,()->franchises.create(profile(0)));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM hm_franchisee",Integer.class));}

    @Test void profileLinksTenantEncryptsSensitiveDataAndAuditsReveal(){long id=create();var stored=jdbc.queryForMap("SELECT * FROM hm_franchisee WHERE id=?",id);assertNotEquals("13800138000",stored.get("leader_mobile_ciphertext"));assertNotEquals("6222020000001234",stored.get("settlement_account_ciphertext"));assertEquals("8000",stored.get("leader_mobile_last4"));assertEquals("1234",stored.get("settlement_account_last4"));assertEquals("FRANCHISE",jdbc.queryForObject("SELECT operation_mode FROM hm_tenant_profile WHERE tenant_id=2",String.class));
        var detail=franchises.detail(id);assertFalse(detail.containsKey("leader_mobile_ciphertext"));assertEquals("*******8000",detail.get("leader_mobile_masked"));var sensitive=franchises.reveal(id,"核对首次打款资料");assertEquals("13800138000",sensitive.get("leaderMobile"));assertEquals("6222020000001234",sensitive.get("settlementAccount"));String logs=jdbc.queryForObject("SELECT GROUP_CONCAT(CONCAT(before_json,after_json)) FROM hm_franchise_audit_log WHERE franchisee_id=?",String.class,id);assertFalse(logs.contains("13800138000"));assertFalse(logs.contains("6222020000001234"));assertEquals(List.of("PROFILE_CREATED","SENSITIVE_VIEWED"),jdbc.queryForList("SELECT action FROM hm_franchise_audit_log WHERE franchisee_id=? ORDER BY id",String.class,id));}

    @Test void duplicateTenantAndStaleProfileUpdateAreRejected(){long id=create();assertThrows(Exception.class,()->franchises.create(new FranchiseService.Profile(2,"OTHER","另一公司","","王经理","13900139000","","","另一公司","银行","6222020000009999",List.of(new FranchiseService.Region("110000","北京市")),0)));franchises.update(id,profile(0));assertThrows(Exception.class,()->franchises.update(id,profile(0)));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_franchisee",Integer.class));}

    @Test void contractNeedsDepositAndCannotOverlap(){long f=create();LocalDate start=LocalDate.now().plusDays(5),end=start.plusYears(1);long first=franchises.createContract(f,contract("HT-001",start,end,10000,0));assertThrows(Exception.class,()->franchises.activate(f,first,0));long receipt=franchises.recordDeposit(f,first,new FranchiseService.Deposit("RECEIPT",10000,"BANK_TRANSFER",LocalDateTime.now(),"BANK-001","receipt-001","实际到账"));assertEquals(receipt,franchises.recordDeposit(f,first,new FranchiseService.Deposit("RECEIPT",10000,"BANK_TRANSFER",LocalDateTime.now(),"BANK-001","receipt-001","重复请求")));assertThrows(Exception.class,()->franchises.recordDeposit(f,first,new FranchiseService.Deposit("RECEIPT",9000,"BANK_TRANSFER",LocalDateTime.now(),"BANK-001","receipt-001","篡改重试")));franchises.activate(f,first,1);assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM hm_franchise_contract WHERE id=?",String.class,first));
        long overlap=franchises.createContract(f,contract("HT-002",start.plusMonths(1),end.plusMonths(1),0,0));assertThrows(Exception.class,()->franchises.activate(f,overlap,0));assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM hm_franchise_contract WHERE id=?",String.class,overlap));}

    @Test void renewalTerminationDepositDisposalAndFranchiseExitKeepHistory(){long f=create();LocalDate start=LocalDate.now().minusMonths(3),end=LocalDate.now().plusMonths(3);long c=franchises.createContract(f,contract("HT-LIVE",start,end,5000,0));franchises.recordDeposit(f,c,new FranchiseService.Deposit("RECEIPT",5000,"CASH",LocalDateTime.now(),"","cash-1","现金收取"));franchises.activate(f,c,1);long renewal=franchises.renew(f,c,new FranchiseService.Renewal("HT-NEXT",start,end.plusYears(1),end.plusYears(2),0,"续约草稿"));assertEquals(c,jdbc.queryForObject("SELECT previous_contract_id FROM hm_franchise_contract WHERE id=?",Long.class,renewal));
        long version=jdbc.queryForObject("SELECT version FROM hm_franchise_contract WHERE id=?",Long.class,c);franchises.terminateContract(f,c,new FranchiseService.Reason("双方协商提前终止",version));assertThrows(Exception.class,()->franchises.terminateFranchise(f,new FranchiseService.Reason("结束合作",jdbc.queryForObject("SELECT version FROM hm_franchisee WHERE id=?",Long.class,f))));franchises.recordDeposit(f,c,new FranchiseService.Deposit("REFUND",5000,"BANK_TRANSFER",LocalDateTime.now(),"RF-1","refund-1","已原路退还"));long fv=jdbc.queryForObject("SELECT version FROM hm_franchisee WHERE id=?",Long.class,f);franchises.terminateFranchise(f,new FranchiseService.Reason("合同与保证金已结清",fv));assertEquals("TERMINATED",jdbc.queryForObject("SELECT status FROM hm_franchisee WHERE id=?",String.class,f));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_franchise_contract WHERE franchisee_id=?",Integer.class,f));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_franchise_deposit_entry WHERE contract_id=?",Integer.class,c));}

    @Test void expiredContractSuspendsFranchiseWithoutDeletingTenant(){long f=create();long invalid=franchises.createContract(f,contract("HT-PAST",LocalDate.now().minusYears(1),LocalDate.now().minusDays(1),0,0));assertThrows(Exception.class,()->franchises.activate(f,invalid,0));LocalDate start=LocalDate.now().minusDays(1),end=LocalDate.now().plusDays(1);long c=franchises.createContract(f,contract("HT-OLD",start,end,0,0));franchises.activate(f,c,0);jdbc.update("UPDATE hm_franchise_contract SET end_date=? WHERE id=?",LocalDate.now().minusDays(1),c);franchises.expireDueContracts();assertEquals("EXPIRED",jdbc.queryForObject("SELECT status FROM hm_franchise_contract WHERE id=?",String.class,c));assertEquals("SUSPENDED",jdbc.queryForObject("SELECT status FROM hm_franchisee WHERE id=?",String.class,f));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=2",Integer.class));}
}
