package com.hm.homemaking.service;

import com.hm.homemaking.domain.HmCustomerUser;
import com.hm.homemaking.domain.HmMiniLoginUser;

public interface HmAppAuthService
{
    String createToken(HmCustomerUser customerUser, String openId);

    HmMiniLoginUser getLoginUser();

    HmCustomerUser getCurrentCustomer();

    void refreshLogin(HmCustomerUser customerUser, String openId, String token);
}
