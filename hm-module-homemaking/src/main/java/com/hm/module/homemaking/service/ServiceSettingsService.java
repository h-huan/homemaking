package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

/** One optimistic service revision covers its price options and booking policies. */
@Service("hmServiceSettingsService")
public class ServiceSettingsService {
    public record Option(Long id,@NotBlank @Size(max=128) String name,@Min(0) @Max(100000000) int priceCents,
                         @Min(30) @Max(480) int durationMinutes,boolean enabled) {}
    public record Area(Long id,@NotBlank @Size(max=128) String name,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,20}") String districtCode,
                       @Min(0) @Max(100000000) int extraCents) {}
    public record Rule(@Min(0) @Max(10080) int minAdvanceMinutes,@Min(1) @Max(90) int maxAdvanceDays,boolean allowSameDay,
                       @NotNull @Size(min=1,max=48) List<@Pattern(regexp="([01][0-9]|2[0-3]):[03]0") String> starts,
                       boolean allowCancel,boolean allowReschedule,@Min(0) @Max(10080) int cancelBeforeMinutes,
                       @Min(0) @Max(10080) int rescheduleBeforeMinutes) {}
    public record Settings(@Min(0) long version,@NotNull @Size(max=50) List<@Valid Option> skus,
                           @NotNull @Size(max=50) List<@Valid Option> extras,@NotNull @Size(max=100) List<@Valid Area> areas,
                           @NotNull @Valid Rule rule) {}
    private final HmRepository repo;private final PricingService pricing;private final ObjectMapper json;
    public ServiceSettingsService(HmRepository repo,PricingService pricing,ObjectMapper json){this.repo=repo;this.pricing=pricing;this.json=json;}

    public Settings get(long serviceId){
        var service=repo.require("hm_service",serviceId,false);var raw=pricing.rule(serviceId);
        List<String> slots=decode(Objects.toString(raw.get("time_slots_json"),"[]"),new TypeReference<>(){});
        Map<String,Object> policy=decode(Objects.toString(raw.get("cancel_rule_json"),"{}"),new TypeReference<>(){});
        var rule=new Rule(cents(raw,"min_advance_minutes"),cents(raw,"max_advance_days"),!Boolean.FALSE.equals(raw.get("allow_same_day")),
            slots.stream().map(s->s.split("-")[0]).distinct().toList(),!Boolean.FALSE.equals(policy.get("allowCancel")),
            !Boolean.FALSE.equals(policy.get("allowReschedule")),((Number)policy.getOrDefault("cancelBeforeMinutes",120)).intValue(),((Number)policy.getOrDefault("rescheduleBeforeMinutes",120)).intValue());
        var areas=repo.jdbc().queryForList("SELECT a.* FROM hm_service_area_relation r JOIN hm_service_area a ON a.id=r.area_id AND a.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.service_id=? AND a.status='ACTIVE' ORDER BY a.id",repo.tenant(),serviceId)
            .stream().map(a->new Area(number(a,"id"),(String)a.get("name"),(String)a.get("district_code"),cents(a,"extra_cents"))).toList();
        return new Settings(number(service,"version"),options(serviceId,true),options(serviceId,false),areas,rule);
    }
    private List<Option> options(long serviceId,boolean sku){return repo.jdbc().queryForList("SELECT * FROM "+(sku?"hm_service_sku":"hm_service_extra")+" WHERE tenant_id=? AND service_id=? ORDER BY id",repo.tenant(),serviceId).stream()
        .map(r->new Option(number(r,"id"),(String)r.get("name"),cents(r,"price_cents"),sku?cents(r,"duration_minutes"):30,"ACTIVE".equals(r.get("status")))).toList();}

    @Transactional public void save(long serviceId,Settings settings){
        var service=repo.require("hm_service",serviceId,true);check(number(service,"version")==settings.version(),"服务已被修改，请刷新配置后再保存");
        check(settings.rule().minAdvanceMinutes()<settings.rule().maxAdvanceDays()*1440,"提前预约时长必须小于最远可约天数");
        check(new HashSet<>(settings.rule().starts()).size()==settings.rule().starts().size(),"预约时刻不能重复");
        for(String time:settings.rule().starts()){LocalTime parsed=LocalTime.parse(time);check(parsed.getMinute()%30==0&&!parsed.isBefore(LocalTime.of(8,0))&&parsed.isBefore(LocalTime.of(21,0)),"预约时刻须为 08:00 至 20:30 的整点或半点");}
        saveOptions(serviceId,settings.skus(),true);saveOptions(serviceId,settings.extras(),false);
        saveAreas(serviceId,settings.areas());var r=settings.rule();
        String cancel=encode(Map.of("allowCancel",r.allowCancel(),"allowReschedule",r.allowReschedule(),"cancelBeforeMinutes",r.cancelBeforeMinutes(),"rescheduleBeforeMinutes",r.rescheduleBeforeMinutes()));
        repo.jdbc().update("INSERT INTO hm_service_booking_rule(tenant_id,service_id,min_advance_minutes,max_advance_days,allow_same_day,time_slots_json,cancel_rule_json) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE min_advance_minutes=VALUES(min_advance_minutes),max_advance_days=VALUES(max_advance_days),allow_same_day=VALUES(allow_same_day),time_slots_json=VALUES(time_slots_json),cancel_rule_json=VALUES(cancel_rule_json)",repo.tenant(),serviceId,r.minAdvanceMinutes(),r.maxAdvanceDays(),r.allowSameDay(),encode(r.starts()),cancel);
        repo.jdbc().update("UPDATE hm_service SET version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),serviceId);
    }
    private void saveOptions(long serviceId,List<Option> options,boolean sku){
        String table=sku?"hm_service_sku":"hm_service_extra";var seen=new HashSet<Long>();
        // Missing options are retained, disabled, so historical references remain resolvable.
        repo.jdbc().update("UPDATE "+table+" SET status='INACTIVE' WHERE tenant_id=? AND service_id=?",repo.tenant(),serviceId);
        for(var option:options){
            check(!sku||option.durationMinutes()%30==0,"规格时长须为 30 分钟整数倍");check(!sku||option.priceCents()>0,"服务规格价格须大于零");
            var args=new ArrayList<Object>(List.of(option.name(),option.priceCents(),option.enabled()?"ACTIVE":"INACTIVE"));if(sku)args.add(option.durationMinutes());
            if(option.id()==null){args.add(repo.tenant());args.add(serviceId);repo.insert("INSERT INTO "+table+"(name,price_cents,status,"+(sku?"duration_minutes,":"")+"tenant_id,service_id) VALUES("+String.join(",",Collections.nCopies(args.size(),"?"))+")",args.toArray());}
            else{check(seen.add(option.id()),"规格或加项重复");args.add(repo.tenant());args.add(serviceId);args.add(option.id());
                check(repo.jdbc().update("UPDATE "+table+" SET name=?,price_cents=?,status=?"+(sku?",duration_minutes=?":"")+" WHERE tenant_id=? AND service_id=? AND id=?",args.toArray())==1,"规格或加项不属于当前服务");}
        }
    }
    private void saveAreas(long serviceId,List<Area> areas){
        var ids=new HashSet<Long>();var districts=new HashSet<String>();var retained=new ArrayList<Long>();
        for(var area:areas){check(districts.add(area.districtCode()),"同一行政区只能配置一次");Long id=area.id();
            if(id!=null){check(ids.add(id),"区域重复");var rows=repo.jdbc().queryForList("SELECT a.* FROM hm_service_area a JOIN hm_service_area_relation r ON r.area_id=a.id AND r.tenant_id=a.tenant_id WHERE a.tenant_id=? AND r.service_id=? AND a.id=? FOR UPDATE",repo.tenant(),serviceId,id);check(rows.size()==1,"区域不属于当前服务");
                var old=rows.get(0);boolean changed=!Objects.equals(old.get("name"),area.name())||!Objects.equals(old.get("district_code"),area.districtCode())||cents(old,"extra_cents")!=area.extraCents();
                // Imported regions can be shared. Copy on edit rather than changing another service's price.
                if(changed&&repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_service_area_relation WHERE tenant_id=? AND area_id=? AND service_id<>?",Long.class,repo.tenant(),id,serviceId)>0)id=null;
            }
            if(id==null)id=repo.insert("INSERT INTO hm_service_area(tenant_id,name,district_code,extra_cents) VALUES(?,?,?,?)",repo.tenant(),area.name(),area.districtCode(),area.extraCents());
            else repo.jdbc().update("UPDATE hm_service_area SET name=?,district_code=?,extra_cents=?,status='ACTIVE' WHERE tenant_id=? AND id=?",area.name(),area.districtCode(),area.extraCents(),repo.tenant(),id);
            retained.add(id);
        }
        repo.jdbc().update("DELETE FROM hm_service_area_relation WHERE tenant_id=? AND service_id=?",repo.tenant(),serviceId);
        for(Long id:retained)repo.jdbc().update("INSERT INTO hm_service_area_relation(tenant_id,service_id,area_id) VALUES(?,?,?)",repo.tenant(),serviceId,id);
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("服务规则无法保存",e);}}
    private <T> T decode(String text,TypeReference<T> type){try{return json.readValue(text.isBlank()?"{}":text,type);}catch(Exception e){throw new IllegalArgumentException("已有服务规则格式无效",e);}}
}
