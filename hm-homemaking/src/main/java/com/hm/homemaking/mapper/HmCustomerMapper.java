package com.hm.homemaking.mapper;

import com.hm.homemaking.domain.HmCustomerAddress;
import com.hm.homemaking.domain.HmCustomerUser;
import com.hm.homemaking.domain.HmUserAuth;
import java.util.List;

public interface HmCustomerMapper
{
    HmUserAuth selectAuthByTypeAndKey(String authType, String authKey);

    int insertAuth(HmUserAuth auth);

    int updateAuth(HmUserAuth auth);

    HmCustomerUser selectCustomerById(Long customerId);

    int insertCustomer(HmCustomerUser customerUser);

    int updateCustomer(HmCustomerUser customerUser);

    List<HmCustomerAddress> selectAddressList(Long customerId);

    HmCustomerAddress selectAddressById(Long addressId);

    int insertAddress(HmCustomerAddress address);

    int updateAddress(HmCustomerAddress address);

    int deleteAddress(Long addressId);

    int clearDefaultAddress(Long customerId);
}
