package com.hm.module.homemaking.controller.app;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.framework.tenant.core.aop.TenantIgnore;
import com.hm.module.homemaking.service.*;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.module.homemaking.dal.HmRepository.check;

@RestController @RequestMapping("/homemaking/public")
public class HomemakingPublicController {
    private final BrandingService branding;private final IdentityService identity;private final PaymentService payments;private final WechatGateway wechat;private final StringRedisTemplate redis;
    public HomemakingPublicController(BrandingService branding,IdentityService identity,PaymentService payments,WechatGateway wechat,StringRedisTemplate redis){this.branding=branding;this.identity=identity;this.payments=payments;this.wechat=wechat;this.redis=redis;}
    @PermitAll @TenantIgnore @GetMapping("/brand") public CommonResult<?> brand(HttpServletRequest request){return success(branding.byHost(request.getServerName()));}
    public record PaymentNotice(@NotNull @Min(1) Long payOrderId){}
    @PermitAll @TenantIgnore @PostMapping("/payment-callback") public CommonResult<?> callback(@Valid @RequestBody PaymentNotice notice){payments.callback(notice.payOrderId());return success(true);}
    @PermitAll @GetMapping("/wechat/mp-start") public CommonResult<?> start(@RequestParam String appId,HttpServletRequest request,HttpServletResponse response){wechat.app(appId,"MP");
        var brand=branding.byHost(request.getServerName());check(!brand.isEmpty(),"当前域名尚未验证");String state=UUID.randomUUID().toString();
        long tenant=com.hm.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();check(((Number)brand.get("tenant_id")).longValue()==tenant,"域名与租户不匹配");
        redis.opsForValue().set("hm:wechat:state:"+state,tenant+":"+appId,Duration.ofMinutes(5));
        var cookie=new Cookie("HM_WECHAT_STATE",state);cookie.setHttpOnly(true);cookie.setSecure(true);cookie.setPath("/");cookie.setMaxAge(300);cookie.setAttribute("SameSite","Lax");response.addCookie(cookie);
        String callback="https://"+request.getServerName()+"/wechat/callback";
        return success(Map.of("url","https://open.weixin.qq.com/connect/oauth2/authorize?appid="+URLEncoder.encode(appId,StandardCharsets.UTF_8)+"&redirect_uri="+URLEncoder.encode(callback,StandardCharsets.UTF_8)+"&response_type=code&scope=snsapi_base&state="+state+"#wechat_redirect"));
    }
    public record MpLogin(@NotBlank String appId,@NotBlank @Size(max=200) String code,@NotBlank String state){}
    @com.hm.framework.ratelimiter.core.annotation.RateLimiter(time=60,count=15,keyResolver=com.hm.module.homemaking.config.HmLoginRateLimitKeyResolver.class)
    @com.hm.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false)
    @PermitAll @PostMapping("/wechat/mp-login") public CommonResult<?> mp(@Valid @RequestBody MpLogin login,@CookieValue(name="HM_WECHAT_STATE",defaultValue="") String cookie){check(login.state().equals(cookie),"微信登录状态不匹配");
        String expected=com.hm.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId()+":"+login.appId();
        var script=new org.springframework.data.redis.core.script.DefaultRedisScript<Long>("if redis.call('GET',KEYS[1]) == ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class);
        check(Objects.equals(redis.execute(script,List.of("hm:wechat:state:"+login.state()),expected),1L),"微信登录状态已过期");return success(identity.login(login.appId(),"MP",login.code()));}
}
