package com.hm.module.homemaking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hm.module.homemaking.dal.HmRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;
import java.util.Map;
import static com.hm.module.homemaking.dal.HmRepository.*;

/** Calls only the fixed official WeChat API origin. Never accepts an OpenID or UnionID from the client. */
@Service("hmWechatGateway")
public class WechatGateway {
    public record Verified(String appId,String platformId,String openId,String unionId){}
    private final HmRepository repo;private final RestClient http;
    public WechatGateway(HmRepository repo){this.repo=repo;var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofSeconds(5));factory.setReadTimeout(Duration.ofSeconds(10));http=RestClient.builder().baseUrl("https://api.weixin.qq.com").requestFactory(factory).build();}
    public Map<String,Object> app(String appId,String kind){var rows=repo.jdbc().queryForList("SELECT * FROM hm_wechat_app WHERE tenant_id=? AND app_id=? AND kind=? AND enabled=TRUE",repo.tenant(),appId,kind);check(rows.size()==1,"微信应用尚未配置");return rows.get(0);}
    private String secret(Map<String,Object> app){String env=(String)app.get("secret_env");check(env.matches("HM_WECHAT_(MINI|MP)(_[A-Z0-9]+)*_SECRET"),"微信密钥配置无效");String secret=System.getenv(env);check(secret!=null&&!secret.isBlank(),"微信密钥尚未配置");return secret;}
    public Verified verify(String appId,String kind,String code){var app=app(appId,kind);JsonNode data;try{
        if(kind.equals("MINI"))data=http.get().uri(b->b.path("/sns/jscode2session").queryParam("appid",appId).queryParam("secret",secret(app)).queryParam("js_code",code).queryParam("grant_type","authorization_code").build()).retrieve().body(JsonNode.class);
        else data=http.get().uri(b->b.path("/sns/oauth2/access_token").queryParam("appid",appId).queryParam("secret",secret(app)).queryParam("code",code).queryParam("grant_type","authorization_code").build()).retrieve().body(JsonNode.class);
        check(data!=null&&data.path("errcode").asInt(0)==0&&!data.path("openid").asText().isBlank(),"微信登录凭证无效");
        return new Verified(appId,(String)app.get("platform_id"),data.path("openid").asText(),data.path("unionid").asText(""));
        }catch(org.springframework.web.client.RestClientException e){throw new IllegalStateException("微信登录服务暂不可用，请重新登录");}
    }
    public String accessToken(Map<String,Object> app){JsonNode data=http.post().uri("/cgi-bin/stable_token").body(Map.of("grant_type","client_credential","appid",app.get("app_id"),"secret",secret(app),"force_refresh",false)).retrieve().body(JsonNode.class);
        check(data!=null&&!data.path("access_token").asText().isBlank(),"微信服务暂不可用");return data.path("access_token").asText();}
    public JsonNode send(Map<String,Object> app,String path,Map<String,Object> payload){String token=accessToken(app);return http.post().uri(b->b.path(path).queryParam("access_token",token).build()).body(payload).retrieve().body(JsonNode.class);}
    public boolean subscribed(Map<String,Object> app){String token=accessToken(app);
        var result=http.get().uri(b->b.path("/cgi-bin/user/info").queryParam("access_token",token).queryParam("openid",app.get("open_id")).queryParam("lang","zh_CN").build()).retrieve().body(JsonNode.class);
        check(result!=null&&result.path("errcode").asInt(0)==0,"无法确认公众号关注状态");return result.path("subscribe").asInt(0)==1;
    }
    public String phone(String appId,String code){var response=send(app(appId,"MINI"),"/wxa/business/getuserphonenumber",Map.of("code",code));check(response!=null&&response.path("errcode").asInt(-1)==0,"手机号授权无效");return response.path("phone_info").path("purePhoneNumber").asText();}
}
