package com.hm.homemaking.mapper;

import com.hm.homemaking.domain.HmBookingRule;
import com.hm.homemaking.domain.HmServiceArea;
import com.hm.homemaking.domain.HmServiceCategory;
import com.hm.homemaking.domain.HmServiceExtraItem;
import com.hm.homemaking.domain.HmServiceItem;
import com.hm.homemaking.domain.HmServiceSku;
import java.util.List;

public interface HmServiceMapper
{
    List<HmServiceItem> selectServiceList(HmServiceItem query);

    HmServiceItem selectServiceById(Long serviceItemId);

    int insertService(HmServiceItem serviceItem);

    int updateService(HmServiceItem serviceItem);

    int deleteServiceById(Long serviceItemId);

    List<HmServiceSku> selectSkuListByServiceItemId(Long serviceItemId);

    int insertSku(HmServiceSku sku);

    int deleteSkuByServiceItemId(Long serviceItemId);

    List<HmServiceExtraItem> selectExtraItemListByServiceItemId(Long serviceItemId);

    int insertExtraItem(HmServiceExtraItem extraItem);

    int deleteExtraItemByServiceItemId(Long serviceItemId);

    HmBookingRule selectBookingRuleByServiceItemId(Long serviceItemId);

    int insertBookingRule(HmBookingRule bookingRule);

    int updateBookingRule(HmBookingRule bookingRule);

    int deleteBookingRuleByServiceItemId(Long serviceItemId);

    List<Long> selectAreaIdsByServiceItemId(Long serviceItemId);

    int insertServiceAreaRel(Long serviceItemId, Long areaId);

    int deleteServiceAreaRelByServiceItemId(Long serviceItemId);

    List<HmServiceArea> selectAreaList(HmServiceArea query);

    HmServiceArea selectAreaById(Long areaId);

    int insertArea(HmServiceArea area);

    int updateArea(HmServiceArea area);

    List<HmServiceCategory> selectMiniCategoryList();

    List<HmServiceItem> selectMiniHotServiceList();
}
