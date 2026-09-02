package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service("hmCatalogService")
public class CatalogService {
    public record Save(Long id, @NotBlank @Size(max=100) String name, @Size(max=500) String address,
            @Size(max=32) String phone, @Size(max=500) String serviceArea, Long storeId,
            @Size(max=1000) String skills, @Size(max=1000) String avatar, @Size(max=100) String category,
            @Size(max=10000) String description, @Min(1) @Max(100000000) Integer priceCents,
            @Min(30) @Max(480) Integer durationMinutes, @Size(max=1000) String cover,
            @Pattern(regexp="ACTIVE|INACTIVE") String status, @Min(0) Long version) {}
    private final HmRepository repo;
    public CatalogService(HmRepository repo) {this.repo=repo;}
    private String table(String kind) {
        return switch(kind) {case "stores"->"hm_store";case "workers"->"hm_worker";case "services"->"hm_service";
            default->throw new ResponseStatusException(HttpStatus.NOT_FOUND);};
    }
    public Map<String,Object> list(String kind,int page,int size,boolean publicView) {
        size=Math.min(Math.max(size,1),100);page=Math.max(page,1);
        String table=table(kind),where=" WHERE tenant_id=?"+(publicView?" AND status='ACTIVE'":"");
        String columns=publicView? switch(kind){case "workers"->"id,store_id,name,skills,avatar";case "services"->"id,store_id,name,category,description,price_cents,duration_minutes,cover";default->"id,name,address,phone,service_area";} : "*";
        return Map.of("list",repo.jdbc().queryForList("SELECT "+columns+" FROM "+table+where+" ORDER BY id DESC LIMIT ? OFFSET ?",repo.tenant(),size,(page-1)*size),
            "total",repo.jdbc().queryForObject("SELECT COUNT(*) FROM "+table+where,Long.class,repo.tenant()));
    }
    @Transactional
    public long save(String kind,Save request) {
        String table=table(kind);var values=new LinkedHashMap<String,Object>();
        values.put("name",request.name());values.put("status",Objects.requireNonNullElse(request.status(),"ACTIVE"));
        if(kind.equals("stores")) {values.put("address",s(request.address()));values.put("phone",s(request.phone()));values.put("service_area",s(request.serviceArea()));}
        else {
            HmRepository.check(request.storeId()!=null,"请选择门店");repo.require("hm_store",request.storeId(),false);values.put("store_id",request.storeId());
            if(kind.equals("workers")){values.put("phone",s(request.phone()));values.put("skills",s(request.skills()));values.put("avatar",safeUrl(request.avatar()));}
            else {HmRepository.check(request.priceCents()!=null&&request.durationMinutes()!=null&&request.durationMinutes()%30==0,"请设置价格及 30 分钟整数倍的服务时长");
                String category=request.category()==null||request.category().isBlank()?"家政服务":request.category().trim();
                repo.jdbc().update("INSERT INTO hm_service_category(tenant_id,name) VALUES(?,?) ON DUPLICATE KEY UPDATE name=VALUES(name)",repo.tenant(),category);
                values.put("category",category);values.put("description",s(request.description()));values.put("price_cents",request.priceCents());values.put("duration_minutes",request.durationMinutes());values.put("cover",safeUrl(request.cover()));}
        }
        if(request.id()==null){values.put("tenant_id",repo.tenant());return repo.insert("INSERT INTO "+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")",values.values().toArray());}
        var current=repo.require(table,request.id(),true);HmRepository.check(request.version()!=null&&request.version()==HmRepository.number(current,"version"),"记录已更新，请刷新后重试");
        var args=new ArrayList<>(values.values());args.add(repo.tenant());args.add(request.id());args.add(request.version());
        int changed=repo.jdbc().update("UPDATE "+table+" SET "+String.join(",",values.keySet().stream().map(k->k+"=?").toList())+",version=version+1 WHERE tenant_id=? AND id=? AND version=?",args.toArray());
        HmRepository.check(changed==1,"记录已更新");return request.id();
    }
    public static String s(String value){return value==null?"":value;}
    public static String safeUrl(String value){value=s(value);if(!value.isEmpty()&&!value.startsWith("https://")&&!value.matches("/[A-Za-z0-9_./-]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"图片地址须使用 HTTPS 或站内路径");return value;}
}
