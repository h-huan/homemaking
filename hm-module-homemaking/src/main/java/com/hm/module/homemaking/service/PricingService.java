package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.module.homemaking.dal.HmRepository;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmPricingService")
public class PricingService {
    public record Extra(@NotNull Long extraItemId,@Min(1) @Max(50) int quantity){}
    public record Item(String name,int priceCents,int quantity,int totalCents){}
    public record Quote(int totalCents,int durationMinutes,List<Item> items){}
    private final HmRepository repo;private final CustomerAccess access;private final ObjectMapper json;
    public PricingService(HmRepository repo,CustomerAccess access,ObjectMapper json){this.repo=repo;this.access=access;this.json=json;}
    public Quote quote(long serviceId,Long skuId,List<Extra> extras,Long addressId){var service=repo.require("hm_service",serviceId,false);check("ACTIVE".equals(service.get("status")),"服务已下架");
        int price=cents(service,"price_cents"),duration=cents(service,"duration_minutes");String name=(String)service.get("name");
        if(skuId!=null){var sku=repo.jdbc().queryForList("SELECT * FROM hm_service_sku WHERE tenant_id=? AND service_id=? AND id=? AND status='ACTIVE'",repo.tenant(),serviceId,skuId);check(sku.size()==1,"规格不属于当前服务或已停用");price=cents(sku.get(0),"price_cents");duration=cents(sku.get(0),"duration_minutes");name=(String)sku.get(0).get("name");}
        var items=new ArrayList<Item>();items.add(new Item(name,price,1,price));var seen=new HashSet<Long>();
        if(extras!=null){check(extras.size()<=20,"加项过多");for(var extra:extras){check(extra.quantity()>0&&extra.quantity()<=50&&seen.add(extra.extraItemId()),"加项数量无效或重复");var rows=repo.jdbc().queryForList("SELECT * FROM hm_service_extra WHERE tenant_id=? AND service_id=? AND id=? AND status='ACTIVE'",repo.tenant(),serviceId,extra.extraItemId());check(rows.size()==1,"加项不属于当前服务或已停用");var row=rows.get(0);int amount=Math.multiplyExact(cents(row,"price_cents"),extra.quantity());price=Math.addExact(price,amount);items.add(new Item((String)row.get("name"),cents(row,"price_cents"),extra.quantity(),amount));}}
        var areas=repo.jdbc().queryForList("SELECT a.* FROM hm_service_area_relation r JOIN hm_service_area a ON a.id=r.area_id AND a.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.service_id=? AND a.status='ACTIVE'",repo.tenant(),serviceId);
        if(addressId!=null){var address=access.own("hm_customer_address",addressId,false);if(!areas.isEmpty()){var matching=areas.stream().filter(a->Objects.equals(a.get("district_code"),address.get("district_code"))).findFirst();check(matching.isPresent(),"该地址不在服务范围内");int fee=cents(matching.get(),"extra_cents");price=Math.addExact(price,fee);if(fee>0)items.add(new Item("区域服务费",fee,1,fee));}}
        check(price>0&&price<=100000000&&duration>0&&duration<=480,"服务计价配置无效");return new Quote(price,duration,items);
    }
    public Map<String,Object> rule(long serviceId){var rows=repo.jdbc().queryForList("SELECT * FROM hm_service_booking_rule WHERE tenant_id=? AND service_id=?",repo.tenant(),serviceId);return rows.isEmpty()?Map.of("min_advance_minutes",120,"max_advance_days",90,"allow_same_day",true,"time_slots_json","[\"09:00-11:00\",\"13:00-15:00\",\"16:00-18:00\"]"):rows.get(0);}
    public int areaFee(long serviceId,String district){
        var areas=repo.jdbc().queryForList("SELECT a.district_code,a.extra_cents FROM hm_service_area_relation r JOIN hm_service_area a ON a.id=r.area_id AND a.tenant_id=r.tenant_id WHERE r.tenant_id=? AND r.service_id=? AND a.status='ACTIVE'",repo.tenant(),serviceId);
        if(areas.isEmpty())return 0;
        var matching=areas.stream().filter(a->Objects.equals(a.get("district_code"),district)).findFirst();check(matching.isPresent(),"该地址不在服务范围内");return cents(matching.get(),"extra_cents");
    }
    public void validateTime(long serviceId,LocalDateTime start){var rule=rule(serviceId);LocalDateTime now=LocalDateTime.now();
        check(start.isAfter(now.plusMinutes(number(rule,"min_advance_minutes")))&&start.isBefore(now.plusDays(number(rule,"max_advance_days"))),"预约时间超出可预约范围");
        if(Boolean.FALSE.equals(rule.get("allow_same_day")))check(start.toLocalDate().isAfter(now.toLocalDate()),"该服务不支持当天预约");
        String slots=(String)rule.get("time_slots_json");if(slots!=null&&!slots.isBlank())try{List<String> configured=json.readValue(slots,new TypeReference<>(){});check(configured.stream().anyMatch(s->start.toLocalTime().equals(LocalTime.parse(s.split("-")[0]))),"请选择已配置的预约时段");}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("预约规则配置无效");}
    }
    public void validateChange(long serviceId,LocalDateTime original,boolean reschedule){
        var rule=rule(serviceId);int minutes=120;boolean enabled=true;
        Object configured=rule.get("cancel_rule_json");
        if(configured!=null&&!configured.toString().isBlank())try{
            Map<String,Object> policy=json.readValue(configured.toString(),new TypeReference<>(){});
            String key=reschedule?"rescheduleBeforeMinutes":"cancelBeforeMinutes";
            if(policy.get(key) instanceof Number n)minutes=n.intValue();
            enabled=!Boolean.FALSE.equals(policy.get(reschedule?"allowReschedule":"allowCancel"));
        }catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalArgumentException("订单变更规则配置无效");}
        check(enabled&&minutes>=0&&original.isAfter(LocalDateTime.now().plusMinutes(minutes)),"已超过自助取消/改约时间，请联系门店");
    }
}
