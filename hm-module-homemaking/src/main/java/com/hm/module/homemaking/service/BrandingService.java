package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.IDN;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmBrandingService")
public class BrandingService {
    public record Brand(@NotBlank @Size(max=100) String brandName,@Size(max=1000) String website,
            @Size(max=1000) String logo,@Size(max=1000) String favicon,@Pattern(regexp="#[0-9A-Fa-f]{6}") String primaryColor,
            @NotBlank @Size(max=200) String loginTitle,@Size(max=1000) String loginBackground,
            @NotNull @Size(max=10) List<String> homeModules,@Size(max=100) String miniAppId,@Size(max=100) String mpAppId,
            @Pattern(regexp="DIRECT|FRANCHISE") String operationMode,@Size(max=100) String payAppKey,
            @Min(1) @Max(168) int completionConfirmHours,@Min(0) long version){}
    private final HmRepository repo;private final ObjectMapper json;
    @org.springframework.beans.factory.annotation.Autowired(required=false) private QuotaService quotas;
    public BrandingService(HmRepository repo,ObjectMapper json){this.repo=repo;this.json=json;}
    public Map<String,Object> current(){var result=new LinkedHashMap<String,Object>(profile(repo.tenant()));var keys=repo.jdbc().queryForList("SELECT pay_app_key FROM hm_tenant_profile WHERE tenant_id=?",repo.tenant());result.put("pay_app_key",keys.isEmpty()?"":keys.get(0).get("pay_app_key"));return result;}
    private Map<String,Object> profile(long tenant){var rows=repo.jdbc().queryForList("SELECT tenant_id,brand_name,website,logo,favicon,primary_color,login_title,login_background,home_modules,mini_app_id,mp_app_id,operation_mode,completion_confirm_hours,version FROM hm_tenant_profile WHERE tenant_id=?",tenant);
        return rows.isEmpty()?Map.of("tenant_id",tenant,"brand_name","HM 家政","logo","/hm-logo.svg","favicon","/hm-logo.svg","primary_color","#136f63","login_title","欢迎使用 HM 家政","home_modules","[\"services\"]","completion_confirm_hours",48,"version",0):rows.get(0);}
    public Map<String,Object> byHost(String host){String domain=domain(host);var rows=repo.jdbc().queryForList("SELECT d.tenant_id,t.name AS tenant_name FROM hm_tenant_domain d JOIN system_tenant t ON t.id=d.tenant_id WHERE d.domain=? AND d.verified=TRUE AND t.status=0 AND t.deleted=FALSE AND t.expire_time>CURRENT_TIMESTAMP",domain);if(rows.isEmpty())return Map.of();var result=new LinkedHashMap<String,Object>(profile(number(rows.get(0),"tenant_id")));result.put("tenant_name",rows.get(0).get("tenant_name"));return result;}
    @Transactional
    public void save(Brand b){check(Set.of("services","stores","workers","reviews","contact","banners").containsAll(b.homeModules()),"首页模块无效");
        if(b.website()!=null&&!b.website().isEmpty())check(b.website().startsWith("https://"),"官网地址须使用 HTTPS");
        if(quotas!=null&&b.miniAppId()!=null&&!b.miniAppId().isBlank())quotas.feature("mini");if(quotas!=null&&b.mpAppId()!=null&&!b.mpAppId().isBlank())quotas.feature("mp");appBelongs(b.miniAppId(),"MINI");appBelongs(b.mpAppId(),"MP");
        if(b.payAppKey()!=null&&!b.payAppKey().isBlank())check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM pay_app WHERE tenant_id=? AND app_key=? AND deleted=FALSE",Long.class,repo.tenant(),b.payAppKey())==1,"支付应用不属于当前租户");
        String modules;try{modules=json.writeValueAsString(b.homeModules());}catch(Exception e){throw new IllegalArgumentException(e);}
        repo.jdbc().update("INSERT INTO hm_tenant_profile(tenant_id,home_modules) VALUES(?,'[]') ON DUPLICATE KEY UPDATE tenant_id=VALUES(tenant_id)",repo.tenant());
        int changed=repo.jdbc().update("UPDATE hm_tenant_profile SET brand_name=?,website=?,logo=?,favicon=?,primary_color=?,login_title=?,login_background=?,home_modules=?,mini_app_id=?,mp_app_id=?,operation_mode=?,pay_app_key=?,completion_confirm_hours=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND version=?",
            b.brandName(),CatalogService.s(b.website()),CatalogService.safeUrl(b.logo()),CatalogService.safeUrl(b.favicon()),b.primaryColor(),b.loginTitle(),CatalogService.safeUrl(b.loginBackground()),modules,CatalogService.s(b.miniAppId()),CatalogService.s(b.mpAppId()),Objects.requireNonNullElse(b.operationMode(),"DIRECT"),CatalogService.s(b.payAppKey()),b.completionConfirmHours(),repo.tenant(),b.version());check(changed==1,"配置已变更，请刷新后重试");
    }
    private void appBelongs(String id,String kind){if(id==null||id.isBlank())return;check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_wechat_app WHERE tenant_id=? AND app_id=? AND kind=?",Long.class,repo.tenant(),id,kind)==1,"微信应用不属于当前租户");}
    public Map<String,String> requestDomain(String host){if(quotas!=null)quotas.feature("domain");String domain=domain(host);check(domain.contains(".")&&!domain.equals("localhost")&&!domain.matches("[0-9.]+"),"请填写有效域名");String token="hm-verify-"+UUID.randomUUID();
        repo.jdbc().update("INSERT INTO hm_tenant_domain(domain,tenant_id,verification_token) VALUES(?,?,?)",domain,repo.tenant(),token);return Map.of("domain",domain,"record","_hm-verification."+domain,"value",token);}
    public void verifyDomain(String host){String domain=domain(host);var rows=repo.jdbc().queryForList("SELECT verification_token FROM hm_tenant_domain WHERE domain=? AND tenant_id=?",domain,repo.tenant());check(rows.size()==1,"域名尚未申请");
        try{var env=new Hashtable<String,String>();env.put("java.naming.factory.initial","com.sun.jndi.dns.DnsContextFactory");env.put("com.sun.jndi.dns.timeout.initial","2000");env.put("com.sun.jndi.dns.timeout.retries","1");var context=new javax.naming.directory.InitialDirContext(env);
            try{var attribute=context.getAttributes("_hm-verification."+domain,new String[]{"TXT"}).get("TXT");boolean found=false;if(attribute!=null)for(int i=0;i<attribute.size();i++)if(attribute.get(i).toString().replace("\"","").equals(rows.get(0).get("verification_token")))found=true;check(found,"尚未检测到域名验证记录");}
            finally{context.close();}
        }catch(javax.naming.NamingException e){throw new IllegalArgumentException("无法验证 DNS TXT 记录");}
        repo.jdbc().update("UPDATE hm_tenant_domain SET verified=TRUE WHERE domain=? AND tenant_id=?",domain,repo.tenant());
    }
    static String domain(String input){String value=IDN.toASCII(input.trim().toLowerCase(Locale.ROOT));check(value.length()<=253&&value.matches("[a-z0-9.-]+")&&!value.contains(".."),"域名格式无效");return value;}
}
