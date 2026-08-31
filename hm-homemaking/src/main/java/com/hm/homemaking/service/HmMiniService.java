package com.hm.homemaking.service;

import com.hm.homemaking.domain.HmCustomerAddress;
import com.hm.homemaking.domain.HmOrder;
import com.hm.homemaking.dto.MiniBindMobileRequest;
import com.hm.homemaking.dto.MiniLoginRequest;
import com.hm.homemaking.dto.MiniOrderCalcRequest;
import com.hm.homemaking.dto.MiniOrderSubmitRequest;
import com.hm.homemaking.dto.MiniReviewRequest;
import java.util.List;
import java.util.Map;

public interface HmMiniService
{
    Map<String, Object> login(MiniLoginRequest request);

    Map<String, Object> bindMobile(MiniBindMobileRequest request);

    Map<String, Object> getCurrentProfile();

    Map<String, Object> getHomeIndex();

    Map<String, Object> getContentByType(String contentType);

    Map<String, Object> getServiceList(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    Map<String, Object> getServiceDetail(Long serviceItemId);

    List<HmCustomerAddress> listAddresses();

    HmCustomerAddress getAddress(Long addressId);

    int saveAddress(HmCustomerAddress address);

    int deleteAddress(Long addressId);

    Map<String, Object> preCalc(MiniOrderCalcRequest request);

    Map<String, Object> submitOrder(MiniOrderSubmitRequest request);

    Map<String, Object> payOrder(Long orderId);

    Map<String, Object> listOrders(String orderStatus, Integer pageNum, Integer pageSize);

    HmOrder getOrderDetail(Long orderId);

    int cancelOrder(Long orderId, String cancelReason);

    int submitReview(MiniReviewRequest request);

    List<Map<String, Object>> getNavConfig();
}
