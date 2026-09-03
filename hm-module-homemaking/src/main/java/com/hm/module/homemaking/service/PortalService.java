package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmPortalService")
public class PortalService {
    public record Link(@NotBlank @Size(max=40) String label,@NotBlank @Size(max=500) String href) {}
    public record Item(@NotBlank @Size(max=80) String title,@Size(max=500) String text,@Size(max=1000) String image) {}
    public record Module(@NotNull @Pattern(regexp="SERVICES|STORES|WORKERS|TRUST|PROCESS|TESTIMONIALS|ABOUT|CONTACT|FAQ") String type,
                         @NotBlank @Size(max=80) String title,@Size(max=300) String subtitle,boolean enabled,
                         @Size(max=20) List<@Min(1) Long> itemIds,@Size(max=12) List<@Valid Item> items) {}
    public record Config(@NotBlank @Size(max=80) String seoTitle,@Size(max=300) String seoDescription,
                         @Size(max=120) String announcement,@NotNull @Size(max=8) List<@Valid Link> navigation,
                         @NotBlank @Size(max=80) String heroTitle,@Size(max=500) String heroText,@Size(max=1000) String heroImage,
                         @Valid Link primaryAction,@Valid Link secondaryAction,@NotNull @Size(max=12) List<@Valid Module> modules,
                         @Size(max=100) String footerText) {}
    public record Draft(@NotNull @Valid Config config,@Min(0) long version) {}
    public record Publish(@Min(0) long version) {}

    private final HmRepository repo;private final ObjectMapper json;private final QuotaService quotas;
    public PortalService(HmRepository repo,ObjectMapper json,QuotaService quotas){this.repo=repo;this.json=json;this.quotas=quotas;}

    public Map<String,Object> draft(){
        long tenant=repo.tenant();var rows=repo.jdbc().queryForList("SELECT draft_json,published_version,published_at,version FROM hm_portal_site WHERE tenant_id=?",tenant);
        if(rows.isEmpty())return Map.of("config",defaults(),"version",0,"published",false);
        var row=rows.get(0);var result=new LinkedHashMap<String,Object>();result.put("config",read(row.get("draft_json")));result.put("version",row.get("version"));result.put("published",row.get("published_version")!=null);result.put("publishedVersion",row.get("published_version"));result.put("publishedAt",row.get("published_at"));return result;
    }
    @Transactional public void save(Draft request){
        quotas.feature("portal");validate(request.config());long tenant=repo.tenant();String value=write(request.config());
        repo.jdbc().update("INSERT INTO hm_portal_site(tenant_id,draft_json,version) VALUES(?,?,0) ON DUPLICATE KEY UPDATE tenant_id=VALUES(tenant_id)",tenant,write(defaults()));
        int changed=repo.jdbc().update("UPDATE hm_portal_site SET draft_json=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND version=?",value,tenant,request.version());
        check(changed==1,"官网内容已更新，请刷新后重试");
    }
    @Transactional public void publish(Publish request){
        quotas.feature("portal");long tenant=repo.tenant();var rows=repo.jdbc().queryForList("SELECT draft_json,version FROM hm_portal_site WHERE tenant_id=? FOR UPDATE",tenant);check(rows.size()==1,"请先保存官网草稿");var site=rows.get(0);check(number(site,"version")==request.version(),"官网内容已更新，请刷新后重试");
        Config config=read(site.get("draft_json"));validate(config);repo.jdbc().update("UPDATE hm_portal_site SET published_json=draft_json,published_version=version,published_at=CURRENT_TIMESTAMP WHERE tenant_id=?",tenant);
        repo.jdbc().update("INSERT INTO hm_portal_revision(tenant_id,version,config_json,published_by) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE version=VALUES(version)",tenant,request.version(),write(config),SecurityFrameworkUtils.getLoginUserId());
    }
    public Map<String,Object> published(String host){
        String domain=BrandingService.domain(host);var domains=repo.jdbc().queryForList("SELECT d.tenant_id,t.name tenant_name,p.brand_name,p.logo,p.favicon,p.primary_color,p.website,s.published_json,s.published_version,s.published_at FROM hm_tenant_domain d JOIN system_tenant t ON t.id=d.tenant_id JOIN hm_tenant_profile p ON p.tenant_id=d.tenant_id JOIN hm_portal_site s ON s.tenant_id=d.tenant_id WHERE d.domain=? AND d.verified=TRUE AND t.status=0 AND t.deleted=FALSE AND t.expire_time>CURRENT_TIMESTAMP AND s.published_json IS NOT NULL",domain);
        if(domains.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"当前域名尚未发布官网");var site=domains.get(0);long tenant=number(site,"tenant_id");Config config=read(site.get("published_json"));
        com.hm.framework.tenant.core.util.TenantUtils.execute(tenant,()->quotas.feature("portal"));
        var brand=new LinkedHashMap<String,Object>();for(String key:List.of("tenant_name","brand_name","logo","favicon","primary_color","website"))brand.put(key,site.get(key));
        var data=new LinkedHashMap<String,Object>();data.put("brand",brand);data.put("config",config);data.put("catalog",catalog(tenant,config));data.put("publishedVersion",site.get("published_version"));data.put("publishedAt",site.get("published_at"));return data;
    }
    private Map<String,Object> catalog(long tenant,Config config){
        var requested=new LinkedHashMap<String,Set<Long>>();for(var module:config.modules())if(module.itemIds()!=null)requested.computeIfAbsent(module.type(),key->new LinkedHashSet<>()).addAll(module.itemIds());
        var out=new LinkedHashMap<String,Object>();out.put("services",select("hm_service","id,store_id,name,category,description,price_cents,duration_minutes,cover",tenant,requested.get("SERVICES"),8,"status='ACTIVE'"));out.put("stores",select("hm_store","id,name,address,phone,service_area",tenant,requested.get("STORES"),6,"status='ACTIVE'"));out.put("workers",select("hm_worker","id,store_id,name,skills,avatar",tenant,requested.get("WORKERS"),6,"status='ACTIVE'"));
        Set<Long> reviews=requested.get("TESTIMONIALS");String ids=ids(reviews);var args=new ArrayList<Object>();args.add(tenant);String filter=ids.isEmpty()?"":" AND r.id IN ("+ids+")";out.put("reviews",repo.jdbc().queryForList("SELECT r.id,r.rating,r.content,r.created_at,c.nickname FROM hm_review r JOIN hm_customer c ON c.id=r.customer_id WHERE r.tenant_id=? AND r.visible=TRUE"+filter+" ORDER BY r.id DESC LIMIT 12",args.toArray()));return out;
    }
    private List<Map<String,Object>> select(String table,String columns,long tenant,Set<Long> selected,int limit,String status){String values=ids(selected),filter=values.isEmpty()?"":" AND id IN ("+values+")";return repo.jdbc().queryForList("SELECT "+columns+" FROM "+table+" WHERE tenant_id=? AND "+status+filter+" ORDER BY id DESC LIMIT "+limit,tenant);}
    private String ids(Set<Long> values){if(values==null||values.isEmpty())return "";return String.join(",",values.stream().filter(v->v!=null&&v>0).map(String::valueOf).toList());}
    private void validate(Config config){
        CatalogService.safeUrl(config.heroImage());if(config.primaryAction()!=null)safeLink(config.primaryAction().href());if(config.secondaryAction()!=null)safeLink(config.secondaryAction().href());
        for(var link:config.navigation())safeLink(link.href());var types=new HashSet<String>();
        for(var module:config.modules()){
            check(types.add(module.type()),"同类首页模块只能添加一次");
            if(module.items()!=null)for(var item:module.items())CatalogService.safeUrl(item.image());
            String table=switch(module.type()){case "SERVICES"->"hm_service";case "STORES"->"hm_store";case "WORKERS"->"hm_worker";case "TESTIMONIALS"->"hm_review";default->null;};
            if(table!=null&&module.itemIds()!=null)for(Long id:module.itemIds())check(id!=null&&id>0&&repo.jdbc().queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=? AND id=?",Long.class,repo.tenant(),id)==1,"推荐内容不存在或不属于当前租户");
        }
    }
    private void safeLink(String value){
        if(value!=null&&(value.matches("/(?!/)[A-Za-z0-9_./#?=&%-]*")||value.matches("#[A-Za-z0-9_-]+")))return;
        try{var uri=java.net.URI.create(value);check("https".equals(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null,"官网链接须使用 HTTPS、站内路径或页面锚点");}
        catch(IllegalArgumentException|NullPointerException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"官网链接格式无效");}
    }
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("官网配置无法保存",e);}}
    private Config read(Object value){try{return json.readValue(Objects.toString(value),Config.class);}catch(Exception e){throw new IllegalArgumentException("官网配置格式无效",e);}}
    private Config defaults(){return new Config("HM 家政｜安心到家的品质服务","专业、透明、可追踪的到家服务","专业到家服务，预约进度清晰可查",List.of(new Link("服务项目","#services"),new Link("服务保障","#trust"),new Link("关于我们","#about"),new Link("联系我们","#contact")),"认真做好每一次到家服务","从预约、上门到售后，每一步都有标准，每一次服务都有记录。","/assets/hm-hero-home.png",new Link("立即预约","#services"),new Link("了解服务保障","#trust"),List.of(new Module("SERVICES","热门服务","明码标价，按需预约",true,List.of(),List.of()),new Module("TRUST","安心服务的四重保障","看得见的标准，查得到的进度",true,List.of(),List.of(new Item("实名服务","人员身份与技能可核验",""),new Item("准时履约","预约时间与服务进度清晰可查",""),new Item("过程留痕","服务前后凭证由授权用户查看",""),new Item("售后闭环","评价、售后与退款状态全程可追踪",""))),new Module("PROCESS","预约只需四步","简单清楚，不让等待变成负担",true,List.of(),List.of(new Item("选择服务","按品类找到所需项目",""),new Item("确定时间","实时查看可约时段",""),new Item("专业上门","服务人员按标准履约",""),new Item("完成评价","问题可直接发起售后",""))),new Module("ABOUT","把服务标准带到每个家庭","我们相信，好的家政服务应该专业、克制、值得长期信任。",true,List.of(),List.of()),new Module("CONTACT","需要帮助？随时联系我们","服务咨询与售后问题会被认真处理",true,List.of(),List.of())),"HM 家政 · 用标准守护每一次到家服务");}
}
