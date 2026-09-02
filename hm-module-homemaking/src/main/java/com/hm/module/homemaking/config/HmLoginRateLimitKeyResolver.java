package com.hm.module.homemaking.config;
import com.hm.framework.ratelimiter.core.annotation.RateLimiter;
import com.hm.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
@Component
public class HmLoginRateLimitKeyResolver implements RateLimiterKeyResolver {
    @Override public String resolver(JoinPoint point,RateLimiter limit){
        var request=((ServletRequestAttributes)RequestContextHolder.currentRequestAttributes()).getRequest();
        // A changing one-time login code must not create a new quota bucket. Only trust the socket peer.
        return "hm:login:"+point.getSignature().toLongString()+":"+request.getRemoteAddr();
    }
}
