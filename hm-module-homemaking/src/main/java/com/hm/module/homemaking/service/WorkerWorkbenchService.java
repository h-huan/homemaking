package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

/** Self-service queries always derive the worker from the authenticated account. */
@Service
public class WorkerWorkbenchService {
    private final HmRepository repo;
    private final WorkerService workers;
    private final ScheduleService schedules;
    private final WorkerTimeOffService timeOff;
    public WorkerWorkbenchService(HmRepository repo,WorkerService workers,ScheduleService schedules,WorkerTimeOffService timeOff) {
        this.repo=repo;this.workers=workers;this.schedules=schedules;this.timeOff=timeOff;
    }
    public Map<String,Object> calendar(LocalDate from,LocalDate to) {
        long worker=workers.current();var calendar=new LinkedHashMap<>(schedules.calendar(worker,from,to));
        var leaves=timeOff.list(worker,from,to);calendar.put("leaves",leaves);
        var occupied=new HashSet<>(repo.jdbc().queryForList("SELECT starts_at FROM hm_worker_slot WHERE tenant_id=? AND worker_id=? AND starts_at>=? AND starts_at<?",LocalDateTime.class,repo.tenant(),worker,from.atStartOfDay(),to.plusDays(1).atStartOfDay()));
        var free=new ArrayList<Map<String,Object>>();
        for(var shift:(List<Map<String,Object>>)calendar.get("schedules")) {
            LocalDateTime start=OrderService.time(shift.get("starts_at")),end=OrderService.time(shift.get("ends_at"));
            if(start.isBefore(from.atStartOfDay()))start=from.atStartOfDay();if(end.isAfter(to.plusDays(1).atStartOfDay()))end=to.plusDays(1).atStartOfDay();
            LocalDateTime run=null;
            for(var slot=start;slot.isBefore(end);slot=slot.plusMinutes(30)) {
                var at=slot;boolean blocked=!slot.isAfter(LocalDateTime.now())||occupied.contains(slot)||leaves.stream().anyMatch(l->"APPROVED".equals(l.get("status"))&&at.isBefore(OrderService.time(l.get("ends_at")))&&at.plusMinutes(30).isAfter(OrderService.time(l.get("starts_at"))));
                if(!blocked&&run==null)run=slot;
                if(blocked&&run!=null){free.add(Map.of("starts_at",run,"ends_at",slot));run=null;}
            }
            if(run!=null)free.add(Map.of("starts_at",run,"ends_at",end));
        }
        calendar.put("available",free);calendar.put("workerId",worker);return calendar;
    }
    public long request(WorkerTimeOffService.Request request) { return timeOff.request(workers.current(),request); }
    public void cancel(long id,WorkerTimeOffService.Cancel cancel) { timeOff.cancel(workers.current(),id,cancel,true); }
    public Map<String,Object> income(LocalDate from,LocalDate to,String view,int page,int size) {
        long worker=workers.current();check(from!=null&&to!=null&&!to.isBefore(from)&&!to.isAfter(from.plusDays(366)),"收入查询范围最多 366 天");
        check(Set.of("ENTRIES","PENDING","PAID").contains(view),"未知收入视图");page=Math.min(Math.max(page,1),100000);size=Math.min(Math.max(size,1),100);
        String join=" FROM hm_settlement_entry e LEFT JOIN hm_settlement_statement s ON s.id=e.statement_id AND s.tenant_id=e.tenant_id AND s.beneficiary=e.beneficiary AND s.beneficiary_id=e.beneficiary_id";
        String owned=" WHERE e.tenant_id=? AND e.beneficiary='WORKER' AND e.beneficiary_id=?";
        var summary=repo.jdbc().queryForMap("SELECT COALESCE(SUM(CASE WHEN e.created_at>=? AND e.created_at<? THEN e.amount_cents ELSE 0 END),0) AS range_earned_cents,COALESCE(SUM(CASE WHEN s.status IN ('PAID','RECONCILED') THEN e.amount_cents ELSE 0 END),0) AS paid_cents,COALESCE(SUM(CASE WHEN s.status IS NULL OR s.status NOT IN ('PAID','RECONCILED') THEN e.amount_cents ELSE 0 END),0) AS pending_cents"+join+owned,from.atStartOfDay(),to.plusDays(1).atStartOfDay(),repo.tenant(),worker);
        String sql,count;var args=new ArrayList<Object>(List.of(repo.tenant(),worker));
        if(view.equals("PAID")) {
            String where=" FROM hm_settlement_statement WHERE tenant_id=? AND beneficiary='WORKER' AND beneficiary_id=? AND status IN ('PAID','RECONCILED') AND paid_at>=? AND paid_at<?";
            count="SELECT COUNT(*)"+where;sql="SELECT id,period_start,period_end,amount_cents,status,payment_reference,paid_at"+where+" ORDER BY paid_at DESC,id DESC";
            args.add(from.atStartOfDay());args.add(to.plusDays(1).atStartOfDay());
        } else {
            String where=view.equals("PENDING")?" AND (s.status IS NULL OR s.status NOT IN ('PAID','RECONCILED'))":" AND e.created_at>=? AND e.created_at<?";
            if(view.equals("ENTRIES")){args.add(from.atStartOfDay());args.add(to.plusDays(1).atStartOfDay());}
            count="SELECT COUNT(*)"+join+owned+where;
            sql="SELECT e.id,e.order_id,e.amount_cents,e.event_key,e.created_at,e.statement_id,COALESCE(s.status,'UNBILLED') AS statement_status,o.service_name"+join+" LEFT JOIN hm_order o ON o.id=e.order_id AND o.tenant_id=e.tenant_id"+owned+where+" ORDER BY e.id DESC";
        }
        long total=repo.jdbc().queryForObject(count,Long.class,args.toArray());args.add(size);args.add((page-1)*size);
        boolean commission=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_commission_rule WHERE tenant_id=? AND enabled=TRUE AND worker_bps>0",Long.class,repo.tenant())>0;
        return Map.of("summary",summary,"list",repo.jdbc().queryForList(sql+" LIMIT ? OFFSET ?",args.toArray()),"total",total,"commissionConfigured",commission,"workerId",worker);
    }
}
