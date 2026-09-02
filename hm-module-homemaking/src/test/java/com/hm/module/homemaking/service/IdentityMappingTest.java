package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.framework.tenant.core.context.TenantContextHolder;
import com.hm.module.system.service.oauth2.OAuth2TokenService;
import com.hm.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import org.junit.jupiter.api.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class IdentityMappingTest {
    JdbcTemplate jdbc; IdentityService service; TransactionTemplate tx;
    @BeforeEach void setup(){
        var source=new DriverManagerDataSource("jdbc:h2:mem:hm_identity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(source);jdbc.execute("DROP ALL OBJECTS");
        new ResourceDatabasePopulator(new FileSystemResource("../sql/mysql/hm-homemaking.sql")).execute(source);
        var repo=new HmRepository(jdbc);var tokens=mock(OAuth2TokenService.class);
        when(tokens.createAccessToken(anyLong(),eq(1),eq("default"),anyList())).thenAnswer(call->new OAuth2AccessTokenDO().setAccessToken("test-access").setRefreshToken("test-refresh").setExpiresTime(LocalDateTime.now().plusHours(1)));
        service=new IdentityService(repo,mock(WechatGateway.class),tokens,new CustomerAccess(repo));tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    Map<String,Object> login(String app,String platform,String open,String union){return tx.execute(s->service.loginVerified(new WechatGateway.Verified(app,platform,open,union)));}
    @Test void miniAndMpOnSameOpenPlatformShareCustomerAcrossTenants(){var mini=login("mini","platform","mini-open","union");TenantContextHolder.setTenantId(2L);var mp=login("mp","platform","mp-open","union");assertEquals(mini.get("customerId"),mp.get("customerId"));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM hm_customer",Integer.class));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_customer_tenant",Integer.class));}
    @Test void unionIdsAreScopedToOpenPlatform(){assertNotEquals(login("a","platform-a","oa","same").get("customerId"),login("b","platform-b","ob","same").get("customerId"));}
    @Test void openIdRemainsStableWithoutUnionId(){assertEquals(login("mini","p","o","").get("customerId"),login("mini","p","o","").get("customerId"));}
    @Test void conflictingExistingIdentitiesNeverSilentlyMerge(){login("a","p","oa","u1");login("b","p","ob","u2");assertThrows(Exception.class,()->login("a","p","oa","u2"));assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM hm_customer",Integer.class));assertEquals("u1",jdbc.queryForObject("SELECT union_id FROM hm_wechat_identity WHERE app_id='a'",String.class));}
    @Test void disabledCustomerRelationCannotBeReactivatedByLogin(){login("mini","p","o","");jdbc.update("UPDATE hm_customer_tenant SET status='INACTIVE'");assertThrows(Exception.class,()->login("mini","p","o",""));assertEquals("INACTIVE",jdbc.queryForObject("SELECT status FROM hm_customer_tenant",String.class));}
}
