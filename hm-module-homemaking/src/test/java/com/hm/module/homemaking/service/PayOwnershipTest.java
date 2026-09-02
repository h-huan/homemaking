package com.hm.module.homemaking.service;

import com.hm.framework.web.core.util.WebFrameworkUtils;
import com.hm.module.pay.controller.app.order.AppPayOrderController;
import com.hm.module.pay.controller.app.order.vo.AppPayOrderSubmitReqVO;
import com.hm.module.pay.dal.dataobject.order.PayOrderDO;
import com.hm.module.pay.service.order.PayOrderService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayOwnershipTest {
    PayOrderService service; AppPayOrderController controller; PayOrderDO order;
    @BeforeEach void setup(){
        service=mock(PayOrderService.class);controller=new AppPayOrderController();ReflectionTestUtils.setField(controller,"payOrderService",service);
        var request=new MockHttpServletRequest();WebFrameworkUtils.setLoginUserId(request,7L);WebFrameworkUtils.setLoginUserType(request,1);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        order=new PayOrderDO();order.setId(5L);order.setUserId(7L);order.setUserType(1);order.setStatus(0);when(service.getOrder(5L)).thenReturn(order);
    }
    @AfterEach void clear(){RequestContextHolder.resetRequestAttributes();}
    AppPayOrderSubmitReqVO submit(){var request=new AppPayOrderSubmitReqVO();request.setId(5L);return request;}
    @Test void matchingNumericIdFromAdminCannotAccessCustomerPayment(){order.setUserType(2);assertNull(controller.getOrder(5L,null,true).getData());assertThrows(AccessDeniedException.class,()->controller.submitPayOrder(submit()));verify(service,never()).syncOrderQuietly(anyLong());}
    @Test void anotherCustomerCannotSubmitPayment(){order.setUserId(8L);assertThrows(AccessDeniedException.class,()->controller.submitPayOrder(submit()));}
    @Test void ownerlessOrderCannotBeClaimed(){order.setUserId(null);assertNull(controller.getOrder(5L,null,false).getData());assertThrows(AccessDeniedException.class,()->controller.submitPayOrder(submit()));}
    @Test void homemakingCannotBypassVerifiedWechatPaymentRoute(){order.setMerchantOrderId("HM-1-8");assertThrows(AccessDeniedException.class,()->controller.submitPayOrder(submit()));}
}
