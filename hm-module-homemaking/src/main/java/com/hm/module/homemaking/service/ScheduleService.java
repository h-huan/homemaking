package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hm.module.homemaking.controller.BusinessTimeDeserializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmScheduleService")
public class ScheduleService {
    public record Interval(@NotNull @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime startsAt, @NotNull @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime endsAt,
                           @Size(max=500) String reason) {}
    public record Skills(@NotNull @Size(max=200) List<Long> serviceIds,
                         @NotNull @Size(max=200) List<String> districts) {}
    public record Shift(@NotBlank @Size(max=100) String name, @NotNull LocalTime startsAt,
                        @NotNull LocalTime endsAt) {}
    public record ApplyShift(@NotNull Long templateId, @NotEmpty @Size(max=31) List<LocalDate> dates) {}
    private final HmRepository repo;
    private final PricingService pricing;
    private final CustomerAccess customers;
    public ScheduleService(HmRepository repo, PricingService pricing, CustomerAccess customers) {
        this.repo=repo; this.pricing=pricing; this.customers=customers;
    }

    public Map<String,Object> calendar(long worker, LocalDate from, LocalDate to) {
        repo.require("hm_worker",worker,false);
        check(!to.isBefore(from) && to.isBefore(from.plusDays(93)),"日历范围最多 93 天");
        var args=new Object[]{repo.tenant(),worker,to.plusDays(1).atStartOfDay(),from.atStartOfDay()};
        return Map.of("schedules",repo.jdbc().queryForList("SELECT * FROM hm_worker_schedule WHERE tenant_id=? AND worker_id=? AND starts_at<? AND ends_at>? ORDER BY starts_at",args),
                "leaves",repo.jdbc().queryForList("SELECT * FROM hm_worker_leave WHERE tenant_id=? AND worker_id=? AND starts_at<? AND ends_at>? ORDER BY starts_at",args),
                "bookings",repo.jdbc().queryForList("SELECT b.id,b.starts_at,b.ends_at,o.id AS order_id,o.service_name,o.status FROM hm_booking b JOIN hm_order o ON o.tenant_id=b.tenant_id AND o.booking_id=b.id WHERE b.tenant_id=? AND b.worker_id=? AND b.starts_at<? AND b.ends_at>? AND o.status NOT IN ('CANCELLED','REFUNDED') ORDER BY b.starts_at",args),
                "serviceIds",repo.jdbc().queryForList("SELECT service_id FROM hm_worker_skill WHERE tenant_id=? AND worker_id=?",Long.class,repo.tenant(),worker),
                "districts",repo.jdbc().queryForList("SELECT district_code FROM hm_worker_area WHERE tenant_id=? AND worker_id=?",String.class,repo.tenant(),worker));
    }

    @Transactional
    public void skills(long worker, Skills request) {
        var person=repo.require("hm_worker",worker,true);
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_slot WHERE tenant_id=? AND worker_id=? AND starts_at>=?",Long.class,repo.tenant(),worker,LocalDateTime.now())==0,
                "人员仍有待履约预约，请先重新派单后修改技能或区域");
        for(long service:request.serviceIds()) check(number(repo.require("hm_service",service,false),"store_id")==number(person,"store_id"),"技能服务必须属于人员门店");
        for(String district:request.districts()) check(district!=null && district.matches("[0-9]{6,12}|\\*"),"服务区域须为行政区编号，全部区域请明确选择 *");
        repo.jdbc().update("DELETE FROM hm_worker_skill WHERE tenant_id=? AND worker_id=?",repo.tenant(),worker);
        repo.jdbc().update("DELETE FROM hm_worker_area WHERE tenant_id=? AND worker_id=?",repo.tenant(),worker);
        for(long service:new HashSet<>(request.serviceIds())) repo.jdbc().update("INSERT INTO hm_worker_skill VALUES(?,?,?)",repo.tenant(),worker,service);
        for(String district:new HashSet<>(request.districts())) repo.jdbc().update("INSERT INTO hm_worker_area VALUES(?,?,?)",repo.tenant(),worker,district);
    }

    @Transactional
    public long add(long worker, Interval interval, boolean leave) {
        repo.require("hm_worker",worker,true); validateInterval(interval.startsAt(),interval.endsAt());
        check(!interval.startsAt().isBefore(LocalDateTime.now()),"不能新增过去的排班或请假");
        check(!occupied(worker,interval.startsAt(),interval.endsAt(),null),"该时间仍有预约，请先改约或派给其他人员");
        if(!leave) check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_schedule WHERE tenant_id=? AND worker_id=? AND starts_at<? AND ends_at>?",Long.class,repo.tenant(),worker,interval.endsAt(),interval.startsAt())==0,"班次时间重叠");
        return leave ? repo.insert("INSERT INTO hm_worker_leave(tenant_id,worker_id,starts_at,ends_at,reason) VALUES(?,?,?,?,?)",repo.tenant(),worker,interval.startsAt(),interval.endsAt(),CatalogService.s(interval.reason()))
                : repo.insert("INSERT INTO hm_worker_schedule(tenant_id,worker_id,starts_at,ends_at) VALUES(?,?,?,?)",repo.tenant(),worker,interval.startsAt(),interval.endsAt());
    }

    @Transactional
    public void remove(long worker, long id, boolean leave) {
        repo.require("hm_worker",worker,true);
        String table=leave?"hm_worker_leave":"hm_worker_schedule";
        var rows=repo.jdbc().queryForList("SELECT * FROM "+table+" WHERE tenant_id=? AND worker_id=? AND id=?",repo.tenant(),worker,id);
        check(rows.size()==1,"排班记录不存在"); var row=rows.get(0);
        if(!leave) check(!occupied(worker,OrderService.time(row.get("starts_at")),OrderService.time(row.get("ends_at")),null),"班次仍有预约，不能删除");
        repo.jdbc().update("DELETE FROM "+table+" WHERE tenant_id=? AND worker_id=? AND id=?",repo.tenant(),worker,id);
    }

    public List<Map<String,Object>> templates() {return repo.jdbc().queryForList("SELECT * FROM hm_shift_template WHERE tenant_id=? ORDER BY id",repo.tenant());}
    @Transactional public long template(Shift s) {
        validateInterval(LocalDate.now().atTime(s.startsAt()),LocalDate.now().atTime(s.endsAt()));
        return repo.insert("INSERT INTO hm_shift_template(tenant_id,name,starts_at,ends_at) VALUES(?,?,?,?)",repo.tenant(),s.name(),s.startsAt(),s.endsAt());
    }
    @Transactional public void apply(long worker, ApplyShift r) {
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_shift_template WHERE tenant_id=? AND id=?",repo.tenant(),r.templateId());
        check(rows.size()==1,"班次模板不存在");var t=rows.get(0);
        for(LocalDate date:new TreeSet<>(r.dates())) add(worker,new Interval(date.atTime(LocalTime.parse(t.get("starts_at").toString())),date.atTime(LocalTime.parse(t.get("ends_at").toString())),""),false);
    }

    public List<Map<String,Object>> capacity(long service, Long sku, Long worker, LocalDate date, Long address) {
        check(!date.isBefore(LocalDate.now()) && date.isBefore(LocalDate.now().plusDays(91)),"查询日期超出范围");
        var quote=pricing.quote(service,sku,List.of(),address);
        String district=address==null?"":Objects.toString(customers.own("hm_customer_address",address,false).get("district_code"),"");
        var result=new ArrayList<Map<String,Object>>();
        for(int minute=8*60;minute<21*60;minute+=30) {
            LocalDateTime start=date.atStartOfDay().plusMinutes(minute),end=start.plusMinutes(quote.durationMinutes());
            if(end.toLocalTime().isAfter(LocalTime.of(21,0)) || !end.toLocalDate().equals(date)) continue;
            try {pricing.validateTime(service,start);} catch(org.springframework.web.server.ResponseStatusException ex) {continue;}
            int count=0;
            for(var candidate:candidates(service,worker,district)) if(available(number(candidate,"id"),start,end,null)) count++;
            result.add(Map.of("startsAt",start,"endsAt",end,"label",start.toLocalTime()+"-"+end.toLocalTime(),"available",count>0,"capacity",count));
        }
        return result;
    }
    public List<Map<String,Object>> capacityForOrder(long orderId,LocalDate date){
        check(!date.isBefore(LocalDate.now())&&date.isBefore(LocalDate.now().plusDays(91)),"查询日期超出范围");
        var order=customers.own("hm_order",orderId,false);var booking=repo.require("hm_booking",number(order,"booking_id"),false);
        long duration=Duration.between(OrderService.time(booking.get("starts_at")),OrderService.time(booking.get("ends_at"))).toMinutes(),service=number(order,"service_id");var result=new ArrayList<Map<String,Object>>();
        for(int minute=8*60;minute<21*60;minute+=30){LocalDateTime start=date.atStartOfDay().plusMinutes(minute),end=start.plusMinutes(duration);if(end.toLocalTime().isAfter(LocalTime.of(21,0))||!end.toLocalDate().equals(date))continue;try{pricing.validateTime(service,start);}catch(org.springframework.web.server.ResponseStatusException ex){continue;}int count=0;for(var candidate:candidates(service,null,Objects.toString(order.get("district_code"),"")))if(available(number(candidate,"id"),start,end,number(booking,"id")))count++;result.add(Map.of("startsAt",start,"endsAt",end,"label",start.toLocalTime()+"-"+end.toLocalTime(),"available",count>0,"capacity",count));}return result;
    }

    /** Caller holds an order/service transaction. Each candidate is rechecked under its row lock. */
    public long select(long service, Long preferred, String district, LocalDateTime start, LocalDateTime end, Long ignoredBooking) {
        validateInterval(start,end);
        for(var candidate:candidates(service,preferred,district)) {
            long id=number(candidate,"id"); var locked=repo.require("hm_worker",id,true);
            if("ACTIVE".equals(locked.get("status")) && matches(id,service,district) && available(id,start,end,ignoredBooking)) return id;
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"该时段暂无符合技能、区域及排班要求的人员，请选择其他时间");
    }
    private List<Map<String,Object>> candidates(long service, Long worker, String district) {
        var s=repo.require("hm_service",service,false);
        String filter=worker==null?"":" AND w.id=?";
        var args=new ArrayList<Object>(List.of(repo.tenant(),number(s,"store_id"),service,district));if(worker!=null)args.add(worker);
        return repo.jdbc().queryForList("SELECT w.id FROM hm_worker w WHERE w.tenant_id=? AND w.store_id=? AND w.status='ACTIVE' AND EXISTS(SELECT 1 FROM hm_worker_skill s WHERE s.tenant_id=w.tenant_id AND s.worker_id=w.id AND s.service_id=?) AND EXISTS(SELECT 1 FROM hm_worker_area a WHERE a.tenant_id=w.tenant_id AND a.worker_id=w.id AND a.district_code IN ('*',?))"+filter+" ORDER BY w.id",args.toArray());
    }
    private boolean matches(long worker,long service,String district) {return !candidates(service,worker,district).isEmpty();}
    private boolean available(long worker,LocalDateTime start,LocalDateTime end,Long ignoredBooking) {
        return repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_schedule WHERE tenant_id=? AND worker_id=? AND starts_at<=? AND ends_at>=?",Long.class,repo.tenant(),worker,start,end)>0
                && repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_leave WHERE tenant_id=? AND worker_id=? AND status='APPROVED' AND starts_at<? AND ends_at>?",Long.class,repo.tenant(),worker,end,start)==0
                && !occupied(worker,start,end,ignoredBooking);
    }
    private boolean occupied(long worker,LocalDateTime start,LocalDateTime end,Long ignoredBooking) {
        return repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_slot WHERE tenant_id=? AND worker_id=? AND starts_at>=? AND starts_at<? AND booking_id<>?",Long.class,repo.tenant(),worker,start,end,ignoredBooking==null?-1:ignoredBooking)>0;
    }
    private static void validateInterval(LocalDateTime start,LocalDateTime end) {
        check(start!=null&&end!=null&&start.isBefore(end)&&start.toLocalDate().equals(end.toLocalDate()),"班次须在同一天且结束晚于开始");
        check(start.getMinute()%30==0&&end.getMinute()%30==0&&start.getSecond()==0&&end.getSecond()==0&&start.getNano()==0&&end.getNano()==0,"时间须以半小时为单位");
    }
}
