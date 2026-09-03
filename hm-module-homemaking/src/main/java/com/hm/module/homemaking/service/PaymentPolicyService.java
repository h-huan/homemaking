package com.hm.module.homemaking.service;

import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.pay.service.app.PayAppService;
import com.hm.module.pay.service.channel.PayChannelService;
import com.hm.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service
public class PaymentPolicyService {
    public record Settings(@NotNull @Pattern(regexp="OFFLINE|ONLINE|BOTH") String mode,@Min(0) long version) {}
    public record Options(String mode, boolean onlineAvailable, boolean offlineAvailable, String message) {}
    private final HmRepository repo; private final PayAppService apps; private final PayChannelService channels; private final Validator validator;
    public PaymentPolicyService(HmRepository repo,PayAppService apps,PayChannelService channels,Validator validator){this.repo=repo;this.apps=apps;this.channels=channels;this.validator=validator;}
    public Options options(){
        var rows=repo.jdbc().queryForList("SELECT payment_mode,pay_app_key,mini_app_id FROM hm_tenant_profile WHERE tenant_id=?",repo.tenant());
        if(rows.isEmpty())return new Options("OFFLINE",false,true,"请联系门店线下付款，到账后由工作人员确认");
        var p=rows.get(0);String mode=Objects.toString(p.get("payment_mode"),"OFFLINE");
        boolean online=!mode.equals("OFFLINE") && ready(Objects.toString(p.get("pay_app_key"),""),Objects.toString(p.get("mini_app_id"),""));
        // An unavailable merchant must never strand a customer at a mandatory online checkout.
        boolean offline=!mode.equals("ONLINE") || !online;
        return new Options(mode,online,offline,online ? (offline?"可微信在线支付，也可联系门店线下付款":"可微信在线支付") : "请联系门店线下付款，到账后由工作人员确认");
    }
    private boolean ready(String key,String mini){
        if(key.isBlank()||mini.isBlank())return false;
        if(repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_wechat_app WHERE tenant_id=? AND app_id=? AND kind='MINI' AND enabled=TRUE",Long.class,repo.tenant(),mini)!=1)return false;
        try{
            var app=apps.validPayApp(key);
            if(app==null || repo.jdbc().queryForObject("SELECT COUNT(*) FROM pay_app WHERE id=? AND tenant_id=? AND app_key=? AND deleted=FALSE",Long.class,app.getId(),repo.tenant(),key)!=1)return false;
            var channel=channels.validPayChannel(app.getId(),"wx_lite");
            if(channel==null || !Objects.equals(channel.getTenantId(),repo.tenant()) || !(channel.getConfig() instanceof WxPayClientConfig config) || !mini.equals(config.getAppId()))return false;
            config.validate(validator);return true;
        }catch(RuntimeException e){return false;}
    }
    public Map<String,Object> current(){
        var rows=repo.jdbc().queryForList("SELECT version FROM hm_tenant_profile WHERE tenant_id=?",repo.tenant());
        return Map.of("options",options(),"version",rows.isEmpty()?0:number(rows.get(0),"version"));
    }
    @Transactional public void save(Settings settings){
        repo.jdbc().update("INSERT INTO hm_tenant_profile(tenant_id,home_modules) VALUES(?,'[]') ON DUPLICATE KEY UPDATE tenant_id=VALUES(tenant_id)",repo.tenant());
        check(repo.jdbc().update("UPDATE hm_tenant_profile SET payment_mode=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND version=?",settings.mode(),repo.tenant(),settings.version())==1,"配置已变更，请刷新后重试");
    }
}
