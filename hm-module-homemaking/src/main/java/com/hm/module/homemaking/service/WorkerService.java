package com.hm.module.homemaking.service;

import com.hm.framework.common.enums.UserTypeEnum;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmWorkerService")
public class WorkerService {
    public record Binding(@NotNull Long userId) {}
    public record Action(@NotNull @Pattern(regexp="ACCEPT|REJECT|ARRIVE|START|COMPLETE|EXCEPTION") String action,@Size(max=500) String note) {}
    private final HmRepository repo;private final OrderService orders;private final EvidenceStorage storage;private final CustomerAccess customers;
    @org.springframework.beans.factory.annotation.Autowired private QuotaService quotas;
    @org.springframework.beans.factory.annotation.Autowired private OrderChangeService changes;
    public WorkerService(HmRepository repo,OrderService orders,EvidenceStorage storage,CustomerAccess customers){this.repo=repo;this.orders=orders;this.storage=storage;this.customers=customers;}
    @Transactional public void bind(long worker,Binding b){
        repo.require("hm_worker",worker,true);
        check(repo.jdbc().queryForList("SELECT id FROM system_users WHERE id=? AND tenant_id=? AND deleted=FALSE AND status=0 FOR UPDATE",b.userId(),repo.tenant()).size()==1,"后台用户不存在、已停用或不属于当前租户");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_worker_account WHERE tenant_id=? AND user_id=? AND worker_id<>?",Long.class,repo.tenant(),b.userId(),worker)==0,"该后台账号已绑定其他服务人员，请使用独立账号");
        repo.jdbc().update("INSERT INTO hm_worker_account(tenant_id,worker_id,user_id) VALUES(?,?,?) ON DUPLICATE KEY UPDATE user_id=VALUES(user_id)",repo.tenant(),worker,b.userId());
    }
    public long current(){
        quotas.feature("worker");
        var user=SecurityFrameworkUtils.getLoginUser();
        check(user!=null&&UserTypeEnum.ADMIN.getValue().equals(user.getUserType()),"请使用服务人员后台账号登录");
        var rows=repo.jdbc().queryForList("SELECT a.worker_id FROM hm_worker_account a JOIN hm_worker w ON w.tenant_id=a.tenant_id AND w.id=a.worker_id WHERE a.tenant_id=? AND a.user_id=? AND w.status='ACTIVE'",repo.tenant(),user.getId());
        check(rows.size()==1,"当前账号未绑定唯一服务人员");return number(rows.get(0),"worker_id");
    }
    public Map<String,Object> tasks(LocalDate from,LocalDate to){
        long worker=current();check(!to.isBefore(from)&&to.isBefore(from.plusDays(32)),"工作台查询范围最多 32 天");
        var rows=repo.jdbc().queryForList("SELECT o.id,o.service_name,o.status,o.fulfillment_status,o.customer_remark,b.starts_at,b.ends_at,o.address FROM hm_order o JOIN hm_booking b ON b.tenant_id=o.tenant_id AND b.id=o.booking_id WHERE o.tenant_id=? AND o.worker_id=? AND b.starts_at>=? AND b.starts_at<? AND o.status IN ('PAID','ASSIGNED','IN_SERVICE','COMPLETED') ORDER BY b.starts_at",repo.tenant(),worker,from.atStartOfDay(),to.plusDays(1).atStartOfDay());
        return Map.of("workerId",worker,"tasks",rows);
    }
    @Transactional public void action(long id,Action request){
        long worker=current();var order=repo.require("hm_order",id,true);check(order.get("worker_id")!=null&&number(order,"worker_id")==worker,"订单不属于当前服务人员");
        changes.requireSettled(order);String status=Objects.toString(order.get("fulfillment_status"),"WAITING");
        var booking=repo.require("hm_booking",number(order,"booking_id"),false);LocalDateTime start=OrderService.time(booking.get("starts_at")),end=OrderService.time(booking.get("ends_at")),now=LocalDateTime.now();
        switch(request.action()){
            case "ACCEPT"->{check(status.equals("WAITING")&&Set.of("PAID","ASSIGNED").contains(order.get("status")),"当前订单不可接单");update(id,"ACCEPTED");repo.jdbc().update("UPDATE hm_order SET status='ASSIGNED' WHERE tenant_id=? AND id=?",repo.tenant(),id);}
            case "REJECT"->{check(status.equals("WAITING")&&Set.of("PAID","ASSIGNED").contains(order.get("status"))&&start.isAfter(now),"当前订单不可拒绝");repo.jdbc().update("DELETE FROM hm_worker_slot WHERE tenant_id=? AND booking_id=?",repo.tenant(),booking.get("id"));repo.jdbc().update("UPDATE hm_booking SET worker_id=NULL WHERE tenant_id=? AND id=?",repo.tenant(),booking.get("id"));repo.jdbc().update("UPDATE hm_order SET worker_id=NULL,status='PAID',fulfillment_status='WAITING',version=version+1 WHERE tenant_id=? AND id=?",repo.tenant(),id);}
            case "ARRIVE"->{check(status.equals("ACCEPTED")&&now.isAfter(start.minusHours(4))&&now.isBefore(end),"请在预约前四小时至结束时间内到达打卡");update(id,"ARRIVED");}
            case "START"->{check(status.equals("ARRIVED"),"请先到达打卡");check(evidence(id,"BEFORE")>0,"开始服务前至少上传一张服务前照片");orders.start(id);update(id,"STARTED");}
            case "COMPLETE"->{check(status.equals("STARTED"),"服务尚未开始");check(evidence(id,"AFTER")>0,"完工前至少上传一张服务后照片");orders.complete(id);}
            case "EXCEPTION"->{check(Set.of("PAID","ASSIGNED","IN_SERVICE").contains(order.get("status"))&&Set.of("WAITING","ACCEPTED","ARRIVED","STARTED").contains(status)&&!CatalogService.s(request.note()).isBlank(),"请填写异常说明");}
            default->throw new IllegalArgumentException("未知履约动作");
        }
        orders.log(id,"WORKER_"+request.action(),CatalogService.s(request.note()));
    }
    @Transactional public long uploadEvidence(long id,String phase,String note,byte[] content){
        long worker=current();var order=repo.require("hm_order",id,true);check(order.get("worker_id")!=null&&number(order,"worker_id")==worker,"订单不属于当前服务人员");
        check(Set.of("BEFORE","AFTER").contains(phase)&&CatalogService.s(note).length()<=500,"照片阶段或说明无效");
        changes.requireSettled(order);String status=Objects.toString(order.get("fulfillment_status"),"WAITING");
        check(Set.of("ASSIGNED","IN_SERVICE").contains(order.get("status")),"当前订单不可上传履约照片");
        check(phase.equals("BEFORE")?Set.of("ARRIVED","STARTED").contains(status):status.equals("STARTED"),"当前履约阶段不能上传该照片");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_fulfillment_evidence WHERE tenant_id=? AND order_id=?",Long.class,repo.tenant(),id)<20,"每个订单最多保存 20 张履约照片");
        var file=storage.save(repo.tenant(),content);
        return repo.insert("INSERT INTO hm_fulfillment_evidence(tenant_id,order_id,worker_id,storage_key,content_type,phase,note) VALUES(?,?,?,?,?,?,?)",repo.tenant(),id,worker,file.key(),file.type(),phase,CatalogService.s(note));
    }
    public List<Map<String,Object>> evidence(long id,boolean customer){
        if(customer)customers.own("hm_order",id,false);else {long worker=current();var order=repo.require("hm_order",id,false);check(order.get("worker_id")!=null&&number(order,"worker_id")==worker,"订单不属于当前服务人员");}
        return evidenceFiles(id);
    }
    public List<Map<String,Object>> adminEvidence(long id){repo.require("hm_order",id,false);return evidenceFiles(id);}
    private List<Map<String,Object>> evidenceFiles(long id){
        return repo.jdbc().queryForList("SELECT id,phase,note,created_at FROM hm_fulfillment_evidence WHERE tenant_id=? AND order_id=? ORDER BY id",repo.tenant(),id);
    }
    public org.springframework.http.ResponseEntity<byte[]> content(long order,long evidence,boolean customer,boolean admin){
        if(admin)adminEvidence(order);else evidence(order,customer);
        var rows=repo.jdbc().queryForList("SELECT storage_key,content_type FROM hm_fulfillment_evidence WHERE tenant_id=? AND order_id=? AND id=?",repo.tenant(),order,evidence);check(rows.size()==1,"履约照片不存在");var file=rows.get(0);
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").contentType(org.springframework.http.MediaType.parseMediaType(file.get("content_type").toString())).body(storage.read(file.get("storage_key").toString()));
    }
    private long evidence(long id,String phase){return repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_fulfillment_evidence WHERE tenant_id=? AND order_id=? AND phase=?",Long.class,repo.tenant(),id,phase);}
    private void update(long id,String state){repo.jdbc().update("UPDATE hm_order SET fulfillment_status=?,version=version+1 WHERE tenant_id=? AND id=?",state,repo.tenant(),id);}
}
