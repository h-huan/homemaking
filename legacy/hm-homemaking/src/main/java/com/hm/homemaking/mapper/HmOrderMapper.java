package com.hm.homemaking.mapper;

import com.hm.homemaking.domain.HmOrder;
import com.hm.homemaking.domain.HmOrderAssign;
import com.hm.homemaking.domain.HmOrderItem;
import com.hm.homemaking.domain.HmOrderOperateLog;
import com.hm.homemaking.domain.HmReview;
import java.util.List;

public interface HmOrderMapper
{
    List<HmOrder> selectOrderList(HmOrder query);

    HmOrder selectOrderById(Long orderId);

    HmOrder selectOrderByOrderNo(String orderNo);

    int insertOrder(HmOrder order);

    int updateOrder(HmOrder order);

    int insertOrderItem(HmOrderItem orderItem);

    List<HmOrderItem> selectOrderItems(Long orderId);

    int insertOrderAssign(HmOrderAssign orderAssign);

    HmOrderAssign selectLatestAssign(Long orderId);

    int insertOrderLog(HmOrderOperateLog log);

    List<HmOrderOperateLog> selectOrderLogs(Long orderId);

    int insertReview(HmReview review);

    HmReview selectReviewByOrderId(Long orderId);
}
