package com.hm.module.homemaking.service;

import java.time.LocalDateTime;
import java.util.*;

/** Platform caps are an upper bound; tenant configuration and consent only reduce delivery. */
public record NotificationPolicy(boolean enabled,int dailyLimit,int minIntervalMinutes,int quietStart,int quietEnd,boolean allowSms,List<String> channels) {
    public static NotificationPolicy defaults(){return new NotificationPolicy(true,5,30,22,8,false,List.of("MP","MINI"));}
    public boolean quiet(LocalDateTime now){int hour=now.getHour();return quietStart!=quietEnd&&(quietStart<quietEnd?hour>=quietStart&&hour<quietEnd:hour>=quietStart||hour<quietEnd);}
    public static Decision decide(NotificationPolicy platform,NotificationPolicy tenant,boolean consent,boolean smsConsent,
            boolean urgent,LocalDateTime now,long platformCount,long tenantCount,LocalDateTime lastSent) {
        if(!platform.enabled||!tenant.enabled||!consent)return new Decision(false,false,List.of(),"DISABLED");
        if(platformCount>=platform.dailyLimit||tenantCount>=Math.min(platform.dailyLimit,tenant.dailyLimit))return new Decision(false,true,List.of(),"DAILY_LIMIT");
        if(lastSent!=null&&lastSent.plusMinutes(Math.max(platform.minIntervalMinutes,tenant.minIntervalMinutes)).isAfter(now))return new Decision(false,true,List.of(),"INTERVAL");
        if(!urgent&&(platform.quiet(now)||tenant.quiet(now)))return new Decision(false,true,List.of(),"QUIET_HOURS");
        var channels=tenant.channels.stream().filter(platform.channels::contains).filter(c->!c.equals("SMS")||(platform.allowSms&&tenant.allowSms&&smsConsent)).distinct().toList();
        return new Decision(!channels.isEmpty(),false,channels,channels.isEmpty()?"NO_CHANNEL":"ALLOW");
    }
    public record Decision(boolean allowed,boolean defer,List<String> channels,String reason){}
    public static Decision intersect(Decision global,Decision event){
        if(!global.allowed())return global;
        if(!event.allowed())return event;
        var channels=event.channels().stream().filter(global.channels()::contains).toList();
        return new Decision(!channels.isEmpty(),false,channels,channels.isEmpty()?"NO_CHANNEL":"ALLOW");
    }
}
