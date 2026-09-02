package com.hm.module.homemaking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hm.module.homemaking.dal.HmRepository;
import com.hm.module.system.api.sms.SmsSendApi;
import com.hm.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.hm.module.homemaking.dal.HmRepository.*;

@Service("hmNotificationGateway")
public class NotificationGateway {
    public record Result(String outcome,String providerId,String message){}
    private final HmRepository repo;private final WechatGateway wechat;private final SmsSendApi sms;private final ObjectMapper json;
    public NotificationGateway(HmRepository repo,WechatGateway wechat,SmsSendApi sms,ObjectMapper json){this.repo=repo;this.wechat=wechat;this.sms=sms;this.json=json;}
    public Result send(Map<String,Object> job){
        boolean attempted=false;
        try{
            String channel=(String)job.get("channel");long tenant=repo.tenant(),customer=number(job,"customer_id");
            var templates=repo.jdbc().queryForList("SELECT * FROM hm_notification_template WHERE tenant_id=? AND event_type=? AND channel=? AND enabled=TRUE",tenant,job.get("event_type"),channel);
            if(templates.isEmpty())return rejected("未配置通知模板");var template=templates.get(0);
            Map<String,Object> payload=json.readValue((String)job.get("payload"),new TypeReference<>(){});
            Map<String,String> mapping=json.readValue((String)template.get("field_mapping"),new TypeReference<>(){});var data=new LinkedHashMap<String,Object>();
            for(var entry:mapping.entrySet())data.put(entry.getKey(),Objects.toString(payload.get(entry.getValue()),""));
            if(channel.equals("SMS")){
                String phone=repo.jdbc().queryForObject("SELECT mobile FROM hm_customer WHERE id=?",String.class,customer);if(phone==null||phone.isBlank())return rejected("客户未绑定手机号");
                var request=new SmsSendSingleToUserReqDTO();request.setUserId(customer);request.setMobile(phone);request.setTemplateCode((String)template.get("template_id"));request.setTemplateParams(data);
                attempted=true;long log=sms.sendSingleSmsToMember(request);return new Result("ACCEPTED",Long.toString(log),"短信已进入供应商发送流程");
            }
            var accounts=repo.jdbc().queryForList("SELECT a.*,i.open_id,i.subscribed FROM hm_wechat_app a JOIN hm_wechat_identity i ON i.app_id=a.app_id WHERE a.tenant_id=? AND a.kind=? AND a.enabled=TRUE AND i.customer_id=?",tenant,channel,customer);
            if(accounts.isEmpty())return rejected("客户尚未关联该微信渠道");var app=accounts.get(0);
            if(channel.equals("MP")){boolean subscribed=wechat.subscribed(app);repo.jdbc().update("UPDATE hm_wechat_identity SET subscribed=? WHERE app_id=? AND customer_id=?",subscribed,app.get("app_id"),customer);if(!subscribed)return rejected("公众号未关注");}
            if(channel.equals("MINI")){
                int reserved=repo.jdbc().update("UPDATE hm_notification_consent SET remaining=remaining-1 WHERE tenant_id=? AND customer_id=? AND app_id=? AND template_id=? AND remaining>0",tenant,customer,app.get("app_id"),template.get("template_id"));
                if(reserved!=1)return rejected("缺少小程序订阅授权");
            }
            var fields=new LinkedHashMap<String,Object>();for(var e:data.entrySet())fields.put(e.getKey(),Map.of("value",e.getValue()));
            var body=new LinkedHashMap<String,Object>();body.put("touser",app.get("open_id"));body.put("template_id",template.get("template_id"));body.put("data",fields);
            attempted=true;var result=wechat.send(app,channel.equals("MP")?"/cgi-bin/message/template/send":"/cgi-bin/message/subscribe/send",body);
            if(result==null)return new Result("UNKNOWN","","供应商响应为空，需核实");
            if(result.path("errcode").asInt(-1)!=0)return rejected("供应商拒绝，错误码="+result.path("errcode").asInt());
            return new Result("SENT",result.path("msgid").asText(""),"");
        }catch(Exception e){return attempted?new Result("UNKNOWN","","调用结果不确定，停止自动重发并等待核实"):rejected("渠道配置或授权不可用");}
    }
    private Result rejected(String message){return new Result("REJECTED","",message);}
}
