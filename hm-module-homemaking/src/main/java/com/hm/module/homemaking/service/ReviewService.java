package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service
public class ReviewService {
    public record Draft(@NotNull Long orderId,@Min(1) @Max(5) int serviceRating,@Min(1) @Max(5) int workerRating,
                        @Size(max=8) List<@NotBlank @Size(max=20) String> tags,@NotBlank @Size(max=2000) String content) {}
    public record Reply(@NotBlank @Size(max=1000) String content) {}
    public record Moderation(boolean visible,boolean recommended) {}
    private final HmRepository repo;private final CustomerAccess customers;private final EvidenceStorage storage;private final ObjectMapper json;
    public ReviewService(HmRepository repo,CustomerAccess customers,EvidenceStorage storage,ObjectMapper json){this.repo=repo;this.customers=customers;this.storage=storage;this.json=json;}

    @Transactional public long draft(Draft request){
        var order=customers.own("hm_order",request.orderId(),true);check("COMPLETED".equals(order.get("status")),"完工后才能评价");check(order.get("worker_id")!=null,"订单没有服务人员，无法评价人员");
        var tags=normalize(request.tags());String tagsJson=write(tags);String content=request.content().trim();check(!content.isEmpty(),"请填写评价内容");
        var existing=repo.jdbc().queryForList("SELECT * FROM hm_review WHERE tenant_id=? AND order_id=? FOR UPDATE",repo.tenant(),request.orderId());
        long id;
        if(existing.isEmpty()){
            int rating=(request.serviceRating()+request.workerRating()+1)/2;
            id=repo.insert("INSERT INTO hm_review(tenant_id,customer_id,order_id,rating,content,service_rating,worker_rating,tags_json,status) VALUES(?,?,?,?,?,?,?,?, 'DRAFT')",repo.tenant(),customers.current(),request.orderId(),rating,content,request.serviceRating(),request.workerRating(),tagsJson);
            log(id,request.orderId(),"DRAFT_SAVED","CUSTOMER","首次保存评价草稿");
        }else{
            var review=existing.get(0);check(number(review,"customer_id")==customers.current(),"评价不属于当前客户");check("DRAFT".equals(review.get("status")),"评价已经发布，不能重复提交");id=number(review,"id");
            var keys=repo.jdbc().queryForList("SELECT storage_key FROM hm_review_image WHERE tenant_id=? AND review_id=?",String.class,repo.tenant(),id);
            repo.jdbc().update("DELETE FROM hm_review_image WHERE tenant_id=? AND review_id=?",repo.tenant(),id);storage.deleteAfterCommit(keys);
            int rating=(request.serviceRating()+request.workerRating()+1)/2;
            repo.jdbc().update("UPDATE hm_review SET rating=?,content=?,service_rating=?,worker_rating=?,tags_json=?,visible=FALSE,recommended=FALSE WHERE tenant_id=? AND id=?",rating,content,request.serviceRating(),request.workerRating(),tagsJson,repo.tenant(),id);
            log(id,request.orderId(),"DRAFT_SAVED","CUSTOMER","更新评价草稿并重置待发布图片");
        }
        return id;
    }

    @Transactional public long upload(long id,String requestKey,byte[] bytes){
        var review=customer(id,true);check("DRAFT".equals(review.get("status")),"评价已经发布，不能继续上传图片");String key=Objects.requireNonNullElse(requestKey,"").trim();check(!key.isEmpty()&&key.length()<=100,"图片请求标识无效");
        var prior=repo.jdbc().queryForList("SELECT id FROM hm_review_image WHERE tenant_id=? AND review_id=? AND request_key=?",Long.class,repo.tenant(),id,key);if(!prior.isEmpty())return prior.get(0);
        int count=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_review_image WHERE tenant_id=? AND review_id=?",Integer.class,repo.tenant(),id);check(count<6,"每条评价最多上传 6 张图片");
        var file=storage.save(repo.tenant(),bytes);long image=repo.insert("INSERT INTO hm_review_image(tenant_id,review_id,storage_key,content_type,request_key,sort_order) VALUES(?,?,?,?,?,?)",repo.tenant(),id,file.key(),file.type(),key,count);
        log(id,number(review,"order_id"),"IMAGE_ADDED","CUSTOMER","评价图片 #"+image);return image;
    }

    @Transactional public Map<String,Object> publish(long id){var review=customer(id,true);check("DRAFT".equals(review.get("status")),"评价已经发布");repo.jdbc().update("UPDATE hm_review SET status='PUBLISHED' WHERE tenant_id=? AND id=?",repo.tenant(),id);log(id,number(review,"order_id"),"PUBLISHED","CUSTOMER","评价已发布");return customerDetail(id);}
    public Map<String,Object> customerDetail(long id){return enrich(customer(id,false),false);}
    public Map<String,Object> adminDetail(long id){return enrich(repo.require("hm_review",id,false),true);}
    public List<Map<String,Object>> adminList(){var rows=repo.jdbc().queryForList("SELECT r.*,o.service_name,o.worker_id,o.store_id,c.nickname FROM hm_review r JOIN hm_order o ON o.tenant_id=r.tenant_id AND o.id=r.order_id JOIN hm_customer c ON c.id=r.customer_id WHERE r.tenant_id=? AND r.status='PUBLISHED'"+repo.scope("hm_review","r")+" ORDER BY r.id DESC LIMIT 100",repo.tenant());rows.forEach(row->decode(row,"tags_json","tags"));return rows;}

    @Transactional public void reply(long id,Reply request){var review=repo.require("hm_review",id,true);check("PUBLISHED".equals(review.get("status")),"评价尚未发布");String before=Objects.toString(review.get("reply_content"),"");String value=request.content().trim();check(!value.isEmpty(),"请填写商家回复");repo.jdbc().update("UPDATE hm_review SET reply_content=?,replied_by=?,replied_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",value,SecurityFrameworkUtils.getLoginUserId(),repo.tenant(),id);log(id,number(review,"order_id"),"REPLIED","ADMIN","回复由“"+before+"”改为“"+value+"”");}
    @Transactional public void moderate(long id,Moderation request){var review=repo.require("hm_review",id,true);check("PUBLISHED".equals(review.get("status")),"评价尚未发布");check(!request.recommended()||request.visible(),"推荐评价必须同时允许展示");String before="visible="+review.get("visible")+",recommended="+review.get("recommended");repo.jdbc().update("UPDATE hm_review SET visible=?,recommended=? WHERE tenant_id=? AND id=?",request.visible(),request.recommended(),repo.tenant(),id);log(id,number(review,"order_id"),"MODERATED","ADMIN",before+" -> visible="+request.visible()+",recommended="+request.recommended());}
    public ResponseEntity<byte[]> image(long reviewId,long imageId,boolean admin){var review=admin?repo.require("hm_review",reviewId,false):customer(reviewId,false);var rows=repo.jdbc().queryForList("SELECT storage_key,content_type FROM hm_review_image WHERE tenant_id=? AND review_id=? AND id=?",repo.tenant(),reviewId,imageId);check(rows.size()==1,"评价图片不存在");var file=rows.get(0);return ResponseEntity.ok().header("Cache-Control","private, no-store").header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(file.get("content_type").toString())).body(storage.read(file.get("storage_key").toString()));}

    private Map<String,Object> customer(long id,boolean lock){var rows=repo.jdbc().queryForList("SELECT * FROM hm_review WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),repo.tenant(),id);check(rows.size()==1,"评价不存在");var review=rows.get(0);customers.own("hm_order",number(review,"order_id"),false);check(number(review,"customer_id")==customers.current(),"评价不属于当前客户");return review;}
    private Map<String,Object> enrich(Map<String,Object> review,boolean logs){decode(review,"tags_json","tags");review.put("images",repo.jdbc().queryForList("SELECT id,sort_order,created_at FROM hm_review_image WHERE tenant_id=? AND review_id=? ORDER BY sort_order,id",repo.tenant(),review.get("id")));if(logs)review.put("logs",repo.jdbc().queryForList("SELECT action,actor_id,actor_type,detail,created_at FROM hm_review_log WHERE tenant_id=? AND review_id=? ORDER BY id",repo.tenant(),review.get("id")));return review;}
    private List<String> normalize(List<String> tags){var out=new ArrayList<String>();for(String raw:tags==null?List.<String>of():tags){String tag=raw.trim();check(!tag.isEmpty()&&tag.length()<=20,"评价标签无效");if(!out.contains(tag))out.add(tag);}check(out.size()<=8,"评价标签最多 8 个");return out;}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("评价内容无法保存",e);}}
    private void decode(Map<String,Object> row,String source,String target){try{row.put(target,json.readValue(Objects.toString(row.get(source),"[]"),new TypeReference<List<String>>(){}));}catch(Exception e){row.put(target,List.of());}}
    private void log(long review,long order,String action,String actor,String detail){repo.insert("INSERT INTO hm_review_log(tenant_id,review_id,order_id,action,actor_id,actor_type,detail) VALUES(?,?,?,?,?,?,?)",repo.tenant(),review,order,action,SecurityFrameworkUtils.getLoginUserId(),actor,detail);}
}
