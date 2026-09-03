package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.system.service.oauth2.OAuth2TokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmIdentityService")
public class IdentityService {
    private final HmRepository repo;private final WechatGateway wechat;private final OAuth2TokenService tokens;private final CustomerAccess access;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private QuotaService quotas;
    public IdentityService(HmRepository repo,WechatGateway wechat,OAuth2TokenService tokens,CustomerAccess access){this.repo=repo;this.wechat=wechat;this.tokens=tokens;this.access=access;}
    @Transactional
    public Map<String,Object> login(String appId,String kind,String code){if(quotas!=null)quotas.feature(kind.equals("MINI")?"mini":"mp");return loginVerified(wechat.verify(appId,kind,code));}
    Map<String,Object> loginVerified(WechatGateway.Verified identity){
        var existing=repo.jdbc().queryForList("SELECT customer_id FROM hm_wechat_identity WHERE app_id=? AND open_id=? FOR UPDATE",identity.appId(),identity.openId());
        Long customer=existing.isEmpty()?null:number(existing.get(0),"customer_id");
        if(!identity.unionId().isBlank()){
            var union=repo.jdbc().queryForList("SELECT customer_id FROM hm_wechat_union WHERE platform_id=? AND union_id=? FOR UPDATE",identity.platformId(),identity.unionId());
            if(!union.isEmpty()){long unionCustomer=number(union.get(0),"customer_id");check(customer==null||customer==unionCustomer,"微信身份关联冲突，需要人工核对");customer=unionCustomer;}
        }
        if(customer==null)customer=repo.insert("INSERT INTO hm_customer(nickname) VALUES(?)","微信用户");
        if(!identity.unionId().isBlank()){
            repo.jdbc().update("INSERT INTO hm_wechat_union(platform_id,union_id,customer_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE union_id=VALUES(union_id)",identity.platformId(),identity.unionId(),customer);
            long bound=repo.jdbc().queryForObject("SELECT customer_id FROM hm_wechat_union WHERE platform_id=? AND union_id=?",Long.class,identity.platformId(),identity.unionId());check(bound==customer,"身份正在关联，请重新登录");
        }
        repo.jdbc().update("INSERT INTO hm_wechat_identity(app_id,open_id,customer_id,union_id) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE union_id=VALUES(union_id)",identity.appId(),identity.openId(),customer,identity.unionId());
        long actual=repo.jdbc().queryForObject("SELECT customer_id FROM hm_wechat_identity WHERE app_id=? AND open_id=?",Long.class,identity.appId(),identity.openId());check(actual==customer,"身份正在关联，请重新登录");
        check("ACTIVE".equals(repo.jdbc().queryForObject("SELECT status FROM hm_customer WHERE id=?",String.class,customer)),"客户账号不可用");
        if(quotas!=null&&repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_customer_tenant WHERE tenant_id=? AND customer_id=?",Long.class,repo.tenant(),customer)==0)quotas.checkCreate("customers");
        repo.jdbc().update("INSERT INTO hm_customer_tenant(tenant_id,customer_id) VALUES(?,?) ON DUPLICATE KEY UPDATE customer_id=VALUES(customer_id)",repo.tenant(),customer);
        check("ACTIVE".equals(repo.jdbc().queryForObject("SELECT status FROM hm_customer_tenant WHERE tenant_id=? AND customer_id=?",String.class,repo.tenant(),customer)),"租户客户关系不可用");
        var token=tokens.createAccessToken(customer,1,"default",List.of("homemaking"));
        return Map.of("customerId",customer,"accessToken",token.getAccessToken(),"refreshToken",token.getRefreshToken(),"expiresTime",token.getExpiresTime(),"tenantId",repo.tenant());
    }
    @Transactional
    public void bindPhone(String appId,String code){String phone=wechat.phone(appId,code);check(phone.matches("[0-9]{6,20}"),"手机号格式无效");repo.jdbc().update("UPDATE hm_customer SET mobile=? WHERE id=?",phone,access.current());}
}
