package com.hm.homemaking.service;

import com.hm.homemaking.domain.HmMiniHomeConfig;
import com.hm.homemaking.domain.HmMiniNavConfig;
import com.hm.homemaking.domain.HmOrder;
import com.hm.homemaking.domain.HmPortalContent;
import com.hm.homemaking.domain.HmServiceArea;
import com.hm.homemaking.domain.HmServiceCategory;
import com.hm.homemaking.domain.HmServiceItem;
import com.hm.homemaking.domain.HmWorkerProfile;
import com.hm.homemaking.dto.AdminOrderAssignRequest;
import com.hm.homemaking.dto.AdminOrderCancelRequest;
import com.hm.homemaking.dto.AdminOrderRefundRequest;
import java.util.List;

public interface HmAdminService
{
    List<HmServiceCategory> listCategories(HmServiceCategory query);

    HmServiceCategory getCategory(Long categoryId);

    int saveCategory(HmServiceCategory category, String operator);

    int removeCategory(Long categoryId);

    List<HmServiceArea> listAreas(HmServiceArea query);

    HmServiceArea getArea(Long areaId);

    int saveArea(HmServiceArea area);

    List<HmServiceItem> listServices(HmServiceItem query);

    HmServiceItem getService(Long serviceItemId);

    int saveService(HmServiceItem serviceItem, String operator);

    int removeService(Long serviceItemId);

    List<HmWorkerProfile> listWorkers(HmWorkerProfile query);

    HmWorkerProfile getWorker(Long workerId);

    int saveWorker(HmWorkerProfile workerProfile, String operator);

    List<HmOrder> listOrders(HmOrder query);

    HmOrder getOrder(Long orderId);

    int assignOrder(AdminOrderAssignRequest request, String operator);

    int cancelOrder(AdminOrderCancelRequest request, String operator);

    int refundOrder(AdminOrderRefundRequest request, String operator);

    List<HmMiniHomeConfig> listHomeConfigs();

    int saveHomeConfigs(List<HmMiniHomeConfig> configs);

    List<HmMiniNavConfig> listNavConfigs();

    int saveNavConfigs(List<HmMiniNavConfig> configs);

    List<HmPortalContent> listPortalContents(HmPortalContent query);

    HmPortalContent getPortalContent(Long contentId);

    int savePortalContent(HmPortalContent content);

    int removePortalContent(Long contentId);
}
