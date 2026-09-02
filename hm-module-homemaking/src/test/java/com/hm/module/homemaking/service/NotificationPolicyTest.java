package com.hm.module.homemaking.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NotificationPolicyTest {
    @org.junit.jupiter.api.Test void eventCannotOverrideGlobalDisable(){var result=NotificationPolicy.intersect(new NotificationPolicy.Decision(false,false,java.util.List.of(),"DISABLED"),new NotificationPolicy.Decision(true,false,java.util.List.of("SMS"),"ALLOW"));org.junit.jupiter.api.Assertions.assertFalse(result.allowed());}
    @org.junit.jupiter.api.Test void eventCannotAddAChannelForbiddenGlobally(){var result=NotificationPolicy.intersect(new NotificationPolicy.Decision(true,false,java.util.List.of("MP"),"ALLOW"),new NotificationPolicy.Decision(true,false,java.util.List.of("MP","SMS"),"ALLOW"));org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("MP"),result.channels());}
    private final LocalDateTime day=LocalDateTime.of(2026,9,2,12,0);
    @Test void tenantCannotRaisePlatformDailyCap(){var p=NotificationPolicy.defaults();var t=new NotificationPolicy(true,100,0,0,0,true,List.of("MP","SMS"));var d=NotificationPolicy.decide(p,t,true,true,true,day,5,0,null);assertFalse(d.allowed());assertEquals("DAILY_LIMIT",d.reason());}
    @Test void urgentMessagesStillRespectPlatformInterval(){var p=NotificationPolicy.defaults();assertFalse(NotificationPolicy.decide(p,p,true,true,true,day,0,0,day.minusMinutes(1)).allowed());}
    @Test void normalQuietHoursDeferButUrgentCanPass(){var p=NotificationPolicy.defaults();var night=day.withHour(23);assertTrue(NotificationPolicy.decide(p,p,true,false,false,night,0,0,null).defer());assertTrue(NotificationPolicy.decide(p,p,true,false,true,night,0,0,null).allowed());}
    @Test void smsNeedsPlatformTenantAndCustomerPermission(){var p=new NotificationPolicy(true,10,0,0,0,true,List.of("MP","MINI","SMS"));assertFalse(NotificationPolicy.decide(p,p,true,false,false,day,0,0,null).channels().contains("SMS"));assertTrue(NotificationPolicy.decide(p,p,true,true,false,day,0,0,null).channels().contains("SMS"));assertFalse(NotificationPolicy.decide(NotificationPolicy.defaults(),p,true,true,false,day,0,0,null).channels().contains("SMS"));}
    @Test void customerOptOutSuppressesOrdinaryMessages(){var p=NotificationPolicy.defaults();assertEquals("DISABLED",NotificationPolicy.decide(p,p,false,true,false,day,0,0,null).reason());}
    @Test void platformAndTenantQuietWindowsBothApply(){var p=NotificationPolicy.defaults();var t=new NotificationPolicy(true,5,30,12,14,false,List.of("MP"));assertTrue(NotificationPolicy.decide(p,t,true,false,false,day,0,0,null).defer());}
    @Test void stateMachineRejectsUnpaidCompletion(){assertFalse(OrderState.allows("UNPAID","COMPLETED"));assertTrue(OrderState.allows("IN_SERVICE","COMPLETED"));assertFalse(OrderState.allows("REFUNDED","PAID"));}
}
