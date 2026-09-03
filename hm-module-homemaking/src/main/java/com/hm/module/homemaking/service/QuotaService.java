package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmQuotaService")
public class QuotaService {
    public static final Set<String> FEATURES=Set.of("portal","domain","mini","mp","sms","worker","settlement");
    public static final Set<String> RESOURCES=Set.of("stores","workers","services","customers","orders","sms");
    public record Entitlement(@NotNull Map<String,Long> limits,@NotNull Set<String> features,@Min(0) long version,Long planId) {}
    public record Plan(Long id,@NotBlank @Size(max=100) String name,@NotNull Map<String,Long> limits,@NotNull Set<String> features,boolean enabled,@Min(0) long version) {}
    private final HmRepository repo; private final ObjectMapper json;
    public QuotaService(HmRepository repo,ObjectMapper json){this.repo=repo;this.json=json;}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("配额格式无效");}}
    private Map<String,Long> limits(Object text){try{return json.readValue(text.toString(),new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException("配额配置无效");}}
    private Set<String> features(Object text){try{return json.readValue(text.toString(),new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException("功能配置无效");}}
    private void validate(Map<String,Long> limits,Set<String> features){
        check(RESOURCES.containsAll(limits.keySet())&&FEATURES.containsAll(features),"套餐包含未知配置");
        for(Long value:limits.values())check(value!=null&&value>=-1&&value<=100000000,"配额为 -1（不限）或非负整数");
    }
    private Map<String,Object> row(long tenant,boolean lock){
        if(lock) repo.jdbc().update("INSERT INTO hm_tenant_entitlement(tenant_id,limits_json,features_json) VALUES(?,?,?) ON DUPLICATE KEY UPDATE tenant_id=VALUES(tenant_id)",tenant,"{}",encode(FEATURES));
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_tenant_entitlement WHERE tenant_id=?"+(lock?" FOR UPDATE":""),tenant);
        if(rows.isEmpty())return Map.of("tenant_id",tenant,"limits_json","{}","features_json",encode(FEATURES),"version",0L);
        return rows.get(0);
    }
    private long effectiveLimit(Map<String,Object> row,String resource){
        if(row.get("plan_id")!=null){
            var plans=repo.jdbc().queryForList("SELECT enabled FROM hm_saas_plan WHERE id=?",row.get("plan_id"));
            check(plans.size()==1&&Boolean.TRUE.equals(plans.get(0).get("enabled")),"租户套餐已停用，请联系总部");
        }
        return limits(row.get("limits_json")).getOrDefault(resource,-1L);
    }
    public Map<String,Object> current(){return describe(repo.tenant());}
    public Map<String,Object> describe(long tenant){com.hm.module.homemaking.security.AdminScope.tenant(tenant);
        var r=row(tenant,false);var used=new LinkedHashMap<String,Long>();for(String resource:RESOURCES)used.put(resource,usage(tenant,resource));
        var result=new LinkedHashMap<String,Object>();result.put("tenantId",tenant);result.put("planId",r.get("plan_id"));result.put("version",r.get("version"));result.put("limits",limits(r.get("limits_json")));result.put("features",features(r.get("features_json")));result.put("usage",used);return result;
    }
    /** Resource creation calls this inside its transaction, before inserting the business row. */
    public void checkCreate(String resource){
        check(RESOURCES.contains(resource),"资源类型无效");var r=row(repo.tenant(),true);long limit=effectiveLimit(r,resource);
        check(limit<0||usage(repo.tenant(),resource)<limit,"已达到套餐的 "+resource+" 配额，请联系总部");
    }
    public void feature(String feature){
        feature(repo.tenant(),feature);
    }
    public void feature(long tenant,String feature){com.hm.module.homemaking.security.AdminScope.tenant(tenant);var r=row(tenant,false);effectiveLimit(r,"orders");check(features(r.get("features_json")).contains(feature),"当前套餐未开通此功能："+feature);}
    public boolean reserveSms(long tenant){
        var r=row(tenant,true);long limit=effectiveLimit(r,"sms");
        if(!features(r.get("features_json")).contains("sms")||(limit>=0&&usage(tenant,"sms")>=limit))return false;
        repo.jdbc().update("INSERT INTO hm_usage_counter(tenant_id,resource,period,used) VALUES(?,'sms',?,1) ON DUPLICATE KEY UPDATE used=used+1",tenant,YearMonth.now(ZoneId.of("Asia/Shanghai")).toString());return true;
    }
    private long usage(long tenant,String resource){
        if(resource.equals("sms")){var rows=repo.jdbc().queryForList("SELECT used FROM hm_usage_counter WHERE tenant_id=? AND resource='sms' AND period=?",Long.class,tenant,YearMonth.now(ZoneId.of("Asia/Shanghai")).toString());return rows.isEmpty()?0:rows.get(0);}
        if(resource.equals("orders"))return repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_order WHERE tenant_id=? AND created_at>=?",Long.class,tenant,YearMonth.now(ZoneId.of("Asia/Shanghai")).atDay(1).atStartOfDay());
        String table=switch(resource){case "stores"->"hm_store";case "workers"->"hm_worker";case "services"->"hm_service";case "customers"->"hm_customer_tenant";default->throw new IllegalArgumentException("Unknown resource");};
        return repo.jdbc().queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=?",Long.class,tenant);
    }
    @Transactional public void save(long tenant,Entitlement e){
        com.hm.module.homemaking.security.AdminScope.platformOnly();validate(e.limits(),e.features());
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=? AND deleted=FALSE",Long.class,tenant)==1,"租户不存在");
        var current=row(tenant,true);check(number(current,"version")==e.version(),"配额已更新，请刷新");
        Map<String,Long> limits=e.limits();Set<String> features=e.features();
        if(e.planId()!=null){var plans=repo.jdbc().queryForList("SELECT * FROM hm_saas_plan WHERE id=? AND enabled=TRUE",e.planId());check(plans.size()==1,"套餐不存在或已停用");limits=limits(plans.get(0).get("limits_json"));features=features(plans.get(0).get("features_json"));}
        repo.jdbc().update("UPDATE hm_tenant_entitlement SET plan_id=?,limits_json=?,features_json=?,version=version+1 WHERE tenant_id=?",e.planId(),encode(limits),encode(features),tenant);
    }
    public List<Map<String,Object>> plans(){com.hm.module.homemaking.security.AdminScope.platformOnly();return repo.jdbc().queryForList("SELECT * FROM hm_saas_plan ORDER BY id");}
    @Transactional public long plan(Plan p){
        com.hm.module.homemaking.security.AdminScope.platformOnly();validate(p.limits(),p.features());
        if(p.id()==null)return repo.insert("INSERT INTO hm_saas_plan(name,limits_json,features_json,enabled) VALUES(?,?,?,?)",p.name(),encode(p.limits()),encode(p.features()),p.enabled());
        check(repo.jdbc().update("UPDATE hm_saas_plan SET name=?,limits_json=?,features_json=?,enabled=?,version=version+1 WHERE id=? AND version=?",p.name(),encode(p.limits()),encode(p.features()),p.enabled(),p.id(),p.version())==1,"套餐已更新，请刷新");
        // Existing subscriptions retain their accepted quota snapshot until explicitly reassigned.
        return p.id();
    }
}
