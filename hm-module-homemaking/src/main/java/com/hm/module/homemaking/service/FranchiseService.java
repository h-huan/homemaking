package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.homemaking.security.AdminScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

import static com.hm.module.homemaking.dal.HmRepository.*;

/** Platform-owned franchise profiles. A business tenant remains an ordinary tenant after it is linked. */
@Service
public class FranchiseService {
    public record Region(@NotBlank @Size(max=32) String code,@NotBlank @Size(max=100) String name) {}
    public record Profile(@Min(1) long tenantId,@NotBlank @Size(max=40) String code,
                          @NotBlank @Size(max=160) String companyName,@Size(max=32) String creditCode,
                          @NotBlank @Size(max=80) String leaderName,@Size(max=32) String leaderMobile,
                          @Email @Size(max=160) String contactEmail,@Size(max=500) String registeredAddress,
                          @NotBlank @Size(max=160) String settlementAccountName,
                          @NotBlank @Size(max=160) String settlementBankName,
                          @Size(max=100) String settlementAccount,
                          @NotNull @Size(min=1,max=100) List<@Valid Region> regions,@Min(0) long version) {}
    public record Contract(@NotBlank @Size(max=60) String contractNo,@NotNull LocalDate signedOn,
                           @NotNull LocalDate startDate,@NotNull LocalDate endDate,@Min(0) long depositCents,
                           @Size(max=1000) String remark,@Min(0) long version) {}
    public record Renewal(@NotBlank @Size(max=60) String contractNo,@NotNull LocalDate signedOn,
                          @NotNull LocalDate startDate,@NotNull LocalDate endDate,@Min(0) long depositCents,
                          @Size(max=1000) String remark) {}
    public record Reason(@NotBlank @Size(max=500) String reason,@Min(0) long version) {}
    public record Deposit(@NotBlank String type,@Min(1) long amountCents,@NotBlank String channel,
                          @NotNull LocalDateTime occurredAt,@Size(max=100) String referenceNo,
                          @NotBlank @Size(max=80) String requestKey,@Size(max=500) String note) {}

    private static final Set<String> DEPOSIT_TYPES=Set.of("RECEIPT","REFUND","FORFEIT");
    private static final Set<String> DEPOSIT_CHANNELS=Set.of("CASH","WECHAT_TRANSFER","ALIPAY_TRANSFER","BANK_TRANSFER","OTHER");
    private final HmRepository repo;private final ObjectMapper json;private final SecretKeySpec key;private final SecureRandom random=new SecureRandom();
    public FranchiseService(HmRepository repo,ObjectMapper json,@Value("${mybatis-plus.encryptor.password}") String secret){
        this.repo=repo;this.json=json;
        if(secret==null||secret.length()<16)throw new IllegalStateException("HM_DATA_ENCRYPTION_KEY 至少需要 16 个字符");
        try{this.key=new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)),"AES");}
        catch(Exception e){throw new IllegalStateException("无法初始化加盟结算资料加密",e);}
    }

    public List<Map<String,Object>> tenantCandidates(){
        AdminScope.platformOnly();
        return repo.jdbc().queryForList("SELECT t.id,t.name,t.contact_name,t.status,t.expire_time,p.operation_mode FROM system_tenant t LEFT JOIN hm_tenant_profile p ON p.tenant_id=t.id WHERE t.deleted=FALSE ORDER BY t.id");
    }
    public List<Map<String,Object>> list(){
        AdminScope.platformOnly();
        var rows=repo.jdbc().queryForList("SELECT f.id,f.tenant_id,t.name tenant_name,f.code,f.company_name,f.leader_name,f.leader_mobile_last4,f.contact_email,f.status,f.version,f.updated_at,(SELECT COUNT(*) FROM hm_franchise_contract c WHERE c.franchisee_id=f.id AND c.status='ACTIVE') active_contracts FROM hm_franchisee f JOIN system_tenant t ON t.id=f.tenant_id WHERE t.deleted=FALSE ORDER BY f.id DESC");
        rows.forEach(this::mask);return rows;
    }
    public Map<String,Object> detail(long id){
        AdminScope.platformOnly();var result=new LinkedHashMap<>(franchise(id,false));mask(result);
        result.put("regions",repo.jdbc().queryForList("SELECT region_code,region_name FROM hm_franchise_region WHERE franchisee_id=? ORDER BY region_code",id));
        result.put("contracts",repo.jdbc().queryForList("SELECT * FROM hm_franchise_contract WHERE franchisee_id=? ORDER BY start_date DESC,id DESC",id));
        result.put("depositEntries",repo.jdbc().queryForList("SELECT id,contract_id,entry_type,amount_cents,channel,occurred_at,reference_no,operator_id,note,created_at FROM hm_franchise_deposit_entry WHERE franchisee_id=? ORDER BY id DESC",id));
        result.put("auditLogs",repo.jdbc().queryForList("SELECT action,contract_id,actor_id,before_json,after_json,reason,created_at FROM hm_franchise_audit_log WHERE franchisee_id=? ORDER BY id DESC LIMIT 200",id));
        return result;
    }

    @Transactional public long create(Profile request){
        AdminScope.platformOnly();validateProfile(request,true);
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM system_tenant WHERE id=? AND deleted=FALSE",Integer.class,request.tenantId())==1,"关联租户不存在");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_franchisee WHERE tenant_id=? OR code=?",Integer.class,request.tenantId(),clean(request.code()))==0,"租户或加盟商编号已经关联");
        long actor=actor();long id=repo.insert("INSERT INTO hm_franchisee(tenant_id,code,company_name,credit_code,leader_name,leader_mobile_ciphertext,leader_mobile_last4,contact_email,registered_address,settlement_account_name,settlement_bank_name,settlement_account_ciphertext,settlement_account_last4,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                request.tenantId(),clean(request.code()),clean(request.companyName()),clean(request.creditCode()),clean(request.leaderName()),encrypt(clean(request.leaderMobile())),last4(request.leaderMobile()),clean(request.contactEmail()),clean(request.registeredAddress()),clean(request.settlementAccountName()),clean(request.settlementBankName()),encrypt(clean(request.settlementAccount())),last4(request.settlementAccount()),actor,actor);
        replaceRegions(id,request.tenantId(),request.regions());linkTenant(request.tenantId());var after=franchise(id,false);audit("PROFILE_CREATED",id,null,request.tenantId(),Map.of(),after,"建立加盟商档案");return id;
    }
    @Transactional public void update(long id,Profile request){
        AdminScope.platformOnly();validateProfile(request,false);var before=franchise(id,true);long tenant=number(before,"tenant_id");check(tenant==request.tenantId(),"加盟商关联租户不能变更");
        check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_franchisee WHERE code=? AND id<>?",Integer.class,clean(request.code()),id)==0,"加盟商编号已经使用");
        String mobile=clean(request.leaderMobile()),account=clean(request.settlementAccount());
        int changed=repo.jdbc().update("UPDATE hm_franchisee SET code=?,company_name=?,credit_code=?,leader_name=?,leader_mobile_ciphertext=?,leader_mobile_last4=?,contact_email=?,registered_address=?,settlement_account_name=?,settlement_bank_name=?,settlement_account_ciphertext=?,settlement_account_last4=?,updated_by=?,version=version+1 WHERE id=? AND version=? AND status<>'TERMINATED'",
                clean(request.code()),clean(request.companyName()),clean(request.creditCode()),clean(request.leaderName()),mobile.isEmpty()?before.get("leader_mobile_ciphertext"):encrypt(mobile),mobile.isEmpty()?before.get("leader_mobile_last4"):last4(mobile),clean(request.contactEmail()),clean(request.registeredAddress()),clean(request.settlementAccountName()),clean(request.settlementBankName()),account.isEmpty()?before.get("settlement_account_ciphertext"):encrypt(account),account.isEmpty()?before.get("settlement_account_last4"):last4(account),actor(),id,request.version());
        check(changed==1,"档案已变更或已终止，请刷新后重试");replaceRegions(id,tenant,request.regions());audit("PROFILE_UPDATED",id,null,tenant,before,franchise(id,false),"更新加盟商档案");
    }
    @Transactional public Map<String,Object> reveal(long id,String reason){
        AdminScope.platformOnly();String why=clean(reason);check(!why.isEmpty()&&why.length()<=500,"查看敏感资料必须填写用途");var row=franchise(id,false);
        audit("SENSITIVE_VIEWED",id,null,number(row,"tenant_id"),Map.of(),Map.of("fields",List.of("leaderMobile","settlementAccount")),why);
        return Map.of("leaderMobile",decrypt(row.get("leader_mobile_ciphertext").toString()),"settlementAccount",decrypt(row.get("settlement_account_ciphertext").toString()));
    }

    @Transactional public long createContract(long franchiseeId,Contract request){
        AdminScope.platformOnly();validateContract(request);var f=franchise(franchiseeId,true);check(!"TERMINATED".equals(f.get("status")),"加盟商已终止");uniqueContract(request.contractNo(),0);
        long id=repo.insert("INSERT INTO hm_franchise_contract(franchisee_id,tenant_id,contract_no,signed_on,start_date,end_date,deposit_cents,deposit_status,remark,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?)",franchiseeId,number(f,"tenant_id"),clean(request.contractNo()),request.signedOn(),request.startDate(),request.endDate(),request.depositCents(),request.depositCents()==0?"NOT_REQUIRED":"PENDING",clean(request.remark()),actor(),actor());
        audit("CONTRACT_CREATED",franchiseeId,id,number(f,"tenant_id"),Map.of(),contract(id,false),"新建合同草稿");return id;
    }
    @Transactional public void updateContract(long franchiseeId,long contractId,Contract request){
        AdminScope.platformOnly();validateContract(request);var before=contractOf(franchiseeId,contractId,true);check("DRAFT".equals(before.get("status")),"只有合同草稿可以编辑");uniqueContract(request.contractNo(),contractId);
        long received=depositTotals(contractId)[0];check(request.depositCents()>=received,"保证金金额不能低于已收金额");String deposit=request.depositCents()==0?"NOT_REQUIRED":received==request.depositCents()?"PAID":"PENDING";
        int changed=repo.jdbc().update("UPDATE hm_franchise_contract SET contract_no=?,signed_on=?,start_date=?,end_date=?,deposit_cents=?,deposit_status=?,remark=?,updated_by=?,version=version+1 WHERE id=? AND franchisee_id=? AND version=? AND status='DRAFT'",clean(request.contractNo()),request.signedOn(),request.startDate(),request.endDate(),request.depositCents(),deposit,clean(request.remark()),actor(),contractId,franchiseeId,request.version());
        check(changed==1,"合同已变更，请刷新后重试");audit("CONTRACT_UPDATED",franchiseeId,contractId,number(before,"tenant_id"),before,contract(contractId,false),"更新合同草稿");
    }
    @Transactional public void activate(long franchiseeId,long contractId,long version){
        AdminScope.platformOnly();var before=contractOf(franchiseeId,contractId,true);check("DRAFT".equals(before.get("status")),"只有合同草稿可以生效");check("NOT_REQUIRED".equals(before.get("deposit_status"))||"PAID".equals(before.get("deposit_status")),"保证金尚未足额收取");
        check(!date(before,"end_date").isBefore(LocalDate.now()),"已过期合同不能生效");
        int overlap=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_franchise_contract WHERE tenant_id=? AND id<>? AND status='ACTIVE' AND start_date<=? AND end_date>=?",Integer.class,before.get("tenant_id"),contractId,before.get("end_date"),before.get("start_date"));check(overlap==0,"合同期限与现有生效合同重叠");
        check(repo.jdbc().update("UPDATE hm_franchise_contract SET status='ACTIVE',activated_by=?,activated_at=CURRENT_TIMESTAMP,updated_by=?,version=version+1 WHERE id=? AND version=? AND status='DRAFT'",actor(),actor(),contractId,version)==1,"合同已变更，请刷新后重试");
        repo.jdbc().update("UPDATE hm_franchisee SET status='ACTIVE',updated_by=?,version=version+1 WHERE id=? AND status<>'TERMINATED'",actor(),franchiseeId);audit("CONTRACT_ACTIVATED",franchiseeId,contractId,number(before,"tenant_id"),before,contract(contractId,false),"合同生效");
    }
    @Transactional public long renew(long franchiseeId,long contractId,Renewal request){
        AdminScope.platformOnly();var previous=contractOf(franchiseeId,contractId,true);check(Set.of("ACTIVE","EXPIRED","TERMINATED").contains(previous.get("status")),"当前合同不能续约");
        var input=new Contract(request.contractNo(),request.signedOn(),request.startDate(),request.endDate(),request.depositCents(),request.remark(),0);validateContract(input);check(request.startDate().isAfter(date(previous,"end_date")),"续约合同必须从原合同结束后开始");uniqueContract(request.contractNo(),0);
        long id=repo.insert("INSERT INTO hm_franchise_contract(franchisee_id,tenant_id,contract_no,signed_on,start_date,end_date,deposit_cents,deposit_status,previous_contract_id,remark,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",franchiseeId,previous.get("tenant_id"),clean(request.contractNo()),request.signedOn(),request.startDate(),request.endDate(),request.depositCents(),request.depositCents()==0?"NOT_REQUIRED":"PENDING",contractId,clean(request.remark()),actor(),actor());
        audit("CONTRACT_RENEWED",franchiseeId,id,number(previous,"tenant_id"),Map.of("previousContractId",contractId),contract(id,false),"创建续约合同草稿");return id;
    }
    @Transactional public void terminateContract(long franchiseeId,long contractId,Reason request){
        AdminScope.platformOnly();String reason=clean(request.reason());var before=contractOf(franchiseeId,contractId,true);check("ACTIVE".equals(before.get("status")),"只有生效合同可以提前终止");
        check(repo.jdbc().update("UPDATE hm_franchise_contract SET status='TERMINATED',terminated_by=?,terminated_at=CURRENT_TIMESTAMP,termination_reason=?,updated_by=?,version=version+1 WHERE id=? AND version=? AND status='ACTIVE'",actor(),reason,actor(),contractId,request.version())==1,"合同已变更，请刷新后重试");
        suspendWithoutActive(franchiseeId);audit("CONTRACT_TERMINATED",franchiseeId,contractId,number(before,"tenant_id"),before,contract(contractId,false),reason);
    }
    @Transactional public long recordDeposit(long franchiseeId,long contractId,Deposit request){
        AdminScope.platformOnly();String type=clean(request.type()).toUpperCase(Locale.ROOT),channel=clean(request.channel()).toUpperCase(Locale.ROOT);check(DEPOSIT_TYPES.contains(type),"保证金流水类型无效");check(DEPOSIT_CHANNELS.contains(channel),"保证金收付渠道无效");
        var c=contractOf(franchiseeId,contractId,true);var prior=repo.jdbc().queryForList("SELECT id,entry_type,amount_cents,channel FROM hm_franchise_deposit_entry WHERE contract_id=? AND request_key=?",contractId,clean(request.requestKey()));
        if(!prior.isEmpty()){var saved=prior.get(0);check(type.equals(saved.get("entry_type"))&&request.amountCents()==number(saved,"amount_cents")&&channel.equals(saved.get("channel")),"幂等请求参数与原保证金流水不一致");return number(saved,"id");}
        long[] totals=depositTotals(contractId);long outstanding=totals[0]-totals[1];
        if(type.equals("RECEIPT")){check(Set.of("DRAFT","ACTIVE").contains(c.get("status")),"当前合同不能登记保证金收款");check(totals[0]+request.amountCents()<=number(c,"deposit_cents"),"保证金收款超过合同约定金额");}
        else {check(Set.of("EXPIRED","TERMINATED").contains(c.get("status")),"合同结束后才能处置保证金");check(request.amountCents()==outstanding,"保证金退还或扣罚必须一次结清当前余额");}
        long id=repo.insert("INSERT INTO hm_franchise_deposit_entry(franchisee_id,contract_id,tenant_id,entry_type,amount_cents,channel,occurred_at,reference_no,request_key,operator_id,note) VALUES(?,?,?,?,?,?,?,?,?,?,?)",franchiseeId,contractId,c.get("tenant_id"),type,request.amountCents(),channel,request.occurredAt(),clean(request.referenceNo()),clean(request.requestKey()),actor(),clean(request.note()));
        totals=depositTotals(contractId);String status=number(c,"deposit_cents")==0?"NOT_REQUIRED":totals[0]==number(c,"deposit_cents")?"PAID":"PENDING";if(totals[0]-totals[1]==0&&totals[0]>0)status=type.equals("FORFEIT")?"FORFEITED":"REFUNDED";
        repo.jdbc().update("UPDATE hm_franchise_contract SET deposit_status=?,updated_by=?,version=version+1 WHERE id=?",status,actor(),contractId);audit("DEPOSIT_"+type,franchiseeId,contractId,number(c,"tenant_id"),Map.of("outstandingCents",outstanding),Map.of("entryId",id,"amountCents",request.amountCents(),"channel",channel,"outstandingCents",totals[0]-totals[1]),clean(request.note()));return id;
    }
    @Transactional public void terminateFranchise(long id,Reason request){
        AdminScope.platformOnly();var before=franchise(id,true);check(!"TERMINATED".equals(before.get("status")),"加盟商已经终止");check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_franchise_contract WHERE franchisee_id=? AND status='ACTIVE'",Integer.class,id)==0,"请先终止生效中的合同");
        var contracts=repo.jdbc().queryForList("SELECT id FROM hm_franchise_contract WHERE franchisee_id=?",Long.class,id);for(long c:contracts){long[] totals=depositTotals(c);check(totals[0]==totals[1],"仍有未退还或未扣罚的保证金");}
        check(repo.jdbc().update("UPDATE hm_franchisee SET status='TERMINATED',terminated_by=?,terminated_at=CURRENT_TIMESTAMP,termination_reason=?,updated_by=?,version=version+1 WHERE id=? AND version=? AND status<>'TERMINATED'",actor(),clean(request.reason()),actor(),id,request.version())==1,"档案已变更，请刷新后重试");audit("FRANCHISE_TERMINATED",id,null,number(before,"tenant_id"),before,franchise(id,false),clean(request.reason()));
    }
    @Transactional void expireDueContracts(){
        var rows=repo.jdbc().queryForList("SELECT id,franchisee_id,tenant_id FROM hm_franchise_contract WHERE status='ACTIVE' AND end_date<? ORDER BY id LIMIT 200",LocalDate.now());
        for(var row:rows){long id=number(row,"id"),franchisee=number(row,"franchisee_id");var before=contract(id,false);if(repo.jdbc().update("UPDATE hm_franchise_contract SET status='EXPIRED',updated_by=0,version=version+1 WHERE id=? AND status='ACTIVE'",id)==1){suspendWithoutActive(franchisee);audit("CONTRACT_EXPIRED",franchisee,id,number(row,"tenant_id"),before,contract(id,false),"合同到期自动处理",0);}}
    }

    private void validateProfile(Profile p,boolean create){check(!clean(p.code()).isEmpty()&&!clean(p.companyName()).isEmpty()&&!clean(p.leaderName()).isEmpty(),"加盟商基本资料不完整");if(create){check(!clean(p.leaderMobile()).isEmpty(),"首次建档必须填写负责人手机号");check(!clean(p.settlementAccount()).isEmpty(),"首次建档必须填写结算账号");}check(p.regions()!=null&&!p.regions().isEmpty(),"至少配置一个加盟区域");var codes=new HashSet<String>();for(var r:p.regions()){check(codes.add(clean(r.code())),"加盟区域不能重复");}}
    private void validateContract(Contract c){check(!c.endDate().isBefore(c.startDate()),"合同结束日期不能早于开始日期");check(!c.signedOn().isAfter(c.startDate()),"签署日期不能晚于合同开始日期");}
    private void uniqueContract(String no,long id){check(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_franchise_contract WHERE contract_no=? AND id<>?",Integer.class,clean(no),id)==0,"合同编号已经使用");}
    private Map<String,Object> franchise(long id,boolean lock){var rows=repo.jdbc().queryForList("SELECT f.*,t.name tenant_name FROM hm_franchisee f JOIN system_tenant t ON t.id=f.tenant_id WHERE f.id=?"+(lock?" FOR UPDATE":""),id);check(rows.size()==1,"加盟商不存在");return rows.get(0);}
    private Map<String,Object> contract(long id,boolean lock){var rows=repo.jdbc().queryForList("SELECT * FROM hm_franchise_contract WHERE id=?"+(lock?" FOR UPDATE":""),id);check(rows.size()==1,"合同不存在");return rows.get(0);}
    private Map<String,Object> contractOf(long franchise,long id,boolean lock){var row=contract(id,lock);check(number(row,"franchisee_id")==franchise,"合同不属于该加盟商");return row;}
    private void replaceRegions(long franchise,long tenant,List<Region> regions){repo.jdbc().update("DELETE FROM hm_franchise_region WHERE franchisee_id=?",franchise);for(var r:regions)repo.jdbc().update("INSERT INTO hm_franchise_region(franchisee_id,tenant_id,region_code,region_name) VALUES(?,?,?,?)",franchise,tenant,clean(r.code()),clean(r.name()));}
    private void linkTenant(long tenant){repo.jdbc().update("INSERT INTO hm_tenant_profile(tenant_id,operation_mode,home_modules) VALUES(?,'FRANCHISE','[]') ON DUPLICATE KEY UPDATE operation_mode='FRANCHISE',updated_at=CURRENT_TIMESTAMP",tenant);}
    private void suspendWithoutActive(long franchise){if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_franchise_contract WHERE franchisee_id=? AND status='ACTIVE'",Integer.class,franchise)==0)repo.jdbc().update("UPDATE hm_franchisee SET status='SUSPENDED',updated_by=0,version=version+1 WHERE id=? AND status='ACTIVE'",franchise);}
    private long[] depositTotals(long contract){var row=repo.jdbc().queryForMap("SELECT COALESCE(SUM(CASE WHEN entry_type='RECEIPT' THEN amount_cents ELSE 0 END),0) received,COALESCE(SUM(CASE WHEN entry_type IN ('REFUND','FORFEIT') THEN amount_cents ELSE 0 END),0) disposed FROM hm_franchise_deposit_entry WHERE contract_id=?",contract);return new long[]{number(row,"received"),number(row,"disposed")};}
    private void audit(String action,long franchise,Long contract,long tenant,Object before,Object after,String reason){audit(action,franchise,contract,tenant,before,after,reason,actor());}
    private void audit(String action,long franchise,Long contract,long tenant,Object before,Object after,String reason,long actor){repo.insert("INSERT INTO hm_franchise_audit_log(action,franchisee_id,contract_id,tenant_id,actor_id,before_json,after_json,reason) VALUES(?,?,?,?,?,?,?,?)",action,franchise,contract,tenant,actor,toJson(redact(before)),toJson(redact(after)),clean(reason));}
    private Object redact(Object value){if(!(value instanceof Map<?,?> source))return value;var safe=new LinkedHashMap<String,Object>();for(var e:source.entrySet()){String k=String.valueOf(e.getKey());if(k.contains("ciphertext"))continue;Object v=e.getValue();safe.put(k,v instanceof java.time.temporal.TemporalAccessor?v.toString():v);}return safe;}
    private String toJson(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("无法记录加盟业务审计",e);}}
    private void mask(Map<String,Object> row){if(row.containsKey("leader_mobile_last4"))row.put("leader_mobile_masked","*******"+row.get("leader_mobile_last4"));if(row.containsKey("settlement_account_last4"))row.put("settlement_account_masked","****"+row.get("settlement_account_last4"));row.remove("leader_mobile_ciphertext");row.remove("settlement_account_ciphertext");}
    private String encrypt(String value){try{byte[] iv=new byte[12];random.nextBytes(iv);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] payload=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,payload,0,iv.length);System.arraycopy(encrypted,0,payload,iv.length,encrypted.length);return "v1:"+Base64.getEncoder().encodeToString(payload);}catch(Exception e){throw new IllegalStateException("加盟结算资料加密失败",e);}}
    private String decrypt(String value){try{check(value.startsWith("v1:"),"加盟结算资料密文版本无效");byte[] payload=Base64.getDecoder().decode(value.substring(3)),iv=Arrays.copyOfRange(payload,0,12),encrypted=Arrays.copyOfRange(payload,12,payload.length);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));return new String(cipher.doFinal(encrypted),StandardCharsets.UTF_8);}catch(org.springframework.web.server.ResponseStatusException e){throw e;}catch(Exception e){throw new IllegalStateException("加盟结算资料解密失败，请核对 HM_DATA_ENCRYPTION_KEY",e);}}
    private static String clean(String value){return Objects.toString(value,"").trim();}
    private static String last4(String value){String v=clean(value).replaceAll("\\s+","");check(v.length()>=4,"敏感号码至少需要 4 位");return v.substring(v.length()-4);}
    private static LocalDate date(Map<String,Object> row,String key){Object v=row.get(key);return v instanceof LocalDate d?d:((java.sql.Date)v).toLocalDate();}
    private static long actor(){return SecurityFrameworkUtils.getLoginUserId();}
}
