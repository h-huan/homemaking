package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.controller.BusinessTimeDeserializer;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.AdminScope;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service
public class WorkerTimeOffService {
    public record Request(@NotNull @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime startsAt,
                          @NotNull @JsonDeserialize(using=BusinessTimeDeserializer.class) LocalDateTime endsAt,
                          @NotNull @Pattern(regexp="LEAVE|REST") String kind,
                          @NotBlank @Size(max=500) String reason, @NotBlank @Size(max=64) String requestKey) {}
    public record Decision(@Min(0) long version, boolean approved, @NotBlank @Size(max=500) String reason) {}
    public record Cancel(@Min(0) long version, @NotBlank @Size(max=500) String reason) {}
    private final HmRepository repo;
    public WorkerTimeOffService(HmRepository repo) { this.repo=repo; }
    private long actor() {
        var user=SecurityFrameworkUtils.getLoginUser();
        AdminScope.denyUnless(user!=null && Objects.equals(user.getUserType(),2));return user.getId();
    }
    private void admin() { actor();AdminScope.denyUnless(AdminScope.current()==null || !AdminScope.current().range().equals("SELF")); }
    private void self(long worker) {
        long actor=actor();
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_account WHERE tenant_id=? AND worker_id=? AND user_id=?",Long.class,repo.tenant(),worker,actor)==1,"请使用绑定本人的服务人员账号");
    }
    @Transactional public long request(long worker,Request r) {
        self(worker);return create(worker,r,false);
    }
    @Transactional public long register(long worker,ScheduleService.Interval interval) {
        admin();return create(worker,new Request(interval.startsAt(),interval.endsAt(),"LEAVE",interval.reason(),UUID.randomUUID().toString()),true);
    }
    private long create(long worker,Request r,boolean approved) {
        repo.require("hm_worker",worker,true);long actor=actor();
        String hash=hash(r);var repeated=repo.jdbc().queryForList("SELECT id,requested_by,request_hash FROM hm_worker_leave WHERE tenant_id=? AND worker_id=? AND request_key=?",repo.tenant(),worker,r.requestKey());
        if(!repeated.isEmpty()){var row=repeated.get(0);check(number(row,"requested_by")==actor && hash.equals(row.get("request_hash")),"同一申请编号不能提交不同内容");return number(row,"id");}
        validate(r);check(!r.startsAt().isBefore(LocalDateTime.now()),"请假或休息开始时间必须在未来");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_leave WHERE tenant_id=? AND worker_id=? AND status IN ('PENDING','APPROVED') AND starts_at<? AND ends_at>?",Long.class,repo.tenant(),worker,r.endsAt(),r.startsAt())==0,"已有重叠的请假或休息申请，请先处理原申请");
        if(approved)unoccupied(worker,r.startsAt(),r.endsAt());
        String status=approved?"APPROVED":"PENDING";
        long id=repo.insert("INSERT INTO hm_worker_leave(tenant_id,worker_id,starts_at,ends_at,reason,status,kind,source,requested_by,request_key,request_hash,reviewed_by,reviewed_at,review_note,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
            repo.tenant(),worker,r.startsAt(),r.endsAt(),r.reason().trim(),status,r.kind(),approved?"ADMIN":"WORKER",actor,r.requestKey(),hash,approved?actor:null,approved?LocalDateTime.now():null,approved?r.reason().trim():"");
        log(worker,id,approved?"REGISTER":"REQUEST",null,status,r.reason());return id;
    }
    @Transactional public void review(long worker,long id,Decision decision) {
        admin();var row=lock(worker,id);check(number(row,"version")==decision.version(),"申请已变化，请刷新");
        check("PENDING".equals(row.get("status")),"只有待审核申请可以审批");check(!CatalogService.s(decision.reason()).isBlank(),"请填写处理说明");
        if(decision.approved()){check(OrderService.time(row.get("starts_at")).isAfter(LocalDateTime.now()),"申请时间已开始，请驳回或由申请人撤销");unoccupied(worker,OrderService.time(row.get("starts_at")),OrderService.time(row.get("ends_at")));}
        String next=decision.approved()?"APPROVED":"REJECTED";
        repo.jdbc().update("UPDATE hm_worker_leave SET status=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP,review_note=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND worker_id=? AND id=?",next,actor(),decision.reason().trim(),repo.tenant(),worker,id);
        log(worker,id,decision.approved()?"APPROVE":"REJECT","PENDING",next,decision.reason());
    }
    @Transactional public void cancel(long worker,long id,Cancel cancel,boolean own) {
        if(own)self(worker);else admin();var row=lock(worker,id);
        check(number(row,"version")==cancel.version(),"申请已变化，请刷新");cancelLocked(worker,id,row,cancel.reason());
    }
    @Transactional public void cancelRegistered(long worker,long id) {admin();var row=lock(worker,id);cancelLocked(worker,id,row,"排班中心撤销");}
    private void cancelLocked(long worker,long id,Map<String,Object> row,String reason) {
        String before=row.get("status").toString();check(Set.of("PENDING","APPROVED").contains(before),"申请已结束，不能重复撤销");
        check(before.equals("PENDING") || OrderService.time(row.get("starts_at")).isAfter(LocalDateTime.now()),"已开始或结束的已批准请假不能撤销");
        check(!CatalogService.s(reason).isBlank()&&reason.length()<=500,"请填写撤销原因");
        repo.jdbc().update("UPDATE hm_worker_leave SET status='CANCELLED',cancelled_by=?,cancelled_at=CURRENT_TIMESTAMP,cancel_reason=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND worker_id=? AND id=?",actor(),reason.trim(),repo.tenant(),worker,id);
        log(worker,id,"CANCEL",before,"CANCELLED",reason);
    }
    private Map<String,Object> lock(long worker,long id) {
        // Same lock order as scheduling/booking: worker first, then leave. Approval cannot race a reservation.
        repo.require("hm_worker",worker,true);
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_worker_leave WHERE tenant_id=? AND worker_id=? AND id=? FOR UPDATE",repo.tenant(),worker,id);
        check(rows.size()==1,"申请不存在");return rows.get(0);
    }
    public List<Map<String,Object>> list(long worker,LocalDate from,LocalDate to) {
        repo.require("hm_worker",worker,false);check(from!=null&&to!=null&&!to.isBefore(from)&&to.isBefore(from.plusDays(93)),"查询范围最多 93 天");
        var rows=repo.jdbc().queryForList("SELECT * FROM hm_worker_leave WHERE tenant_id=? AND worker_id=? AND starts_at<? AND ends_at>? ORDER BY starts_at DESC,id DESC",repo.tenant(),worker,to.plusDays(1).atStartOfDay(),from.atStartOfDay());
        var history=repo.jdbc().queryForList("SELECT l.* FROM hm_worker_leave_log l JOIN hm_worker_leave r ON r.id=l.leave_id AND r.tenant_id=l.tenant_id AND r.worker_id=l.worker_id WHERE r.tenant_id=? AND r.worker_id=? AND r.starts_at<? AND r.ends_at>? ORDER BY l.id",repo.tenant(),worker,to.plusDays(1).atStartOfDay(),from.atStartOfDay());
        for(var row:rows){row.remove("request_key");row.remove("request_hash");long id=number(row,"id");row.put("history",history.stream().filter(h->number(h,"leave_id")==id).toList());row.put("can_cancel","PENDING".equals(row.get("status"))||"APPROVED".equals(row.get("status"))&&OrderService.time(row.get("starts_at")).isAfter(LocalDateTime.now()));}
        return rows;
    }
    private void unoccupied(long worker,LocalDateTime start,LocalDateTime end) {
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_slot WHERE tenant_id=? AND worker_id=? AND starts_at>=? AND starts_at<?",Long.class,repo.tenant(),worker,start,end)==0,"该时间已有预约，请先改约或重新派单，再批准请假");
    }
    private void validate(Request r) {
        check(r.startsAt()!=null&&r.endsAt()!=null&&r.startsAt().isBefore(r.endsAt())&&!r.endsAt().isAfter(r.startsAt().plusDays(31))&&r.startsAt().isBefore(LocalDateTime.now().plusDays(91)),"开始须早于结束，单次最多 31 天且开始在未来 90 天内");
        for(var t:List.of(r.startsAt(),r.endsAt()))check(t.getMinute()%30==0&&t.getSecond()==0&&t.getNano()==0,"请按半小时选择时间");
        check(Set.of("LEAVE","REST").contains(r.kind())&&!CatalogService.s(r.reason()).isBlank()&&r.reason().length()<=500,"请填写有效类型与申请原因");
    }
    private String hash(Request r) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((r.startsAt()+"\n"+r.endsAt()+"\n"+r.kind()+"\n"+r.reason()).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private void log(long worker,long id,String action,String from,String to,String reason) {repo.jdbc().update("INSERT INTO hm_worker_leave_log(tenant_id,worker_id,leave_id,action,from_status,to_status,operator_id,reason) VALUES(?,?,?,?,?,?,?,?)",repo.tenant(),worker,id,action,from,to,actor(),reason.trim());}
}
