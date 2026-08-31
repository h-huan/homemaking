package com.hm.homemaking.service.impl;

import com.alibaba.fastjson2.JSON;
import com.hm.common.exception.ServiceException;
import com.hm.common.utils.DateUtils;
import com.hm.common.utils.StringUtils;
import com.hm.common.utils.ip.IpUtils;
import com.hm.homemaking.domain.HmBookingRule;
import com.hm.homemaking.domain.HmCustomerAddress;
import com.hm.homemaking.domain.HmCustomerUser;
import com.hm.homemaking.domain.HmMiniNavConfig;
import com.hm.homemaking.domain.HmOrder;
import com.hm.homemaking.domain.HmOrderAssign;
import com.hm.homemaking.domain.HmOrderItem;
import com.hm.homemaking.domain.HmOrderOperateLog;
import com.hm.homemaking.domain.HmPortalContent;
import com.hm.homemaking.domain.HmReview;
import com.hm.homemaking.domain.HmServiceArea;
import com.hm.homemaking.domain.HmServiceExtraItem;
import com.hm.homemaking.domain.HmServiceItem;
import com.hm.homemaking.domain.HmServiceSku;
import com.hm.homemaking.domain.HmUserAuth;
import com.hm.homemaking.dto.MiniBindMobileRequest;
import com.hm.homemaking.dto.MiniLoginRequest;
import com.hm.homemaking.dto.MiniOrderCalcRequest;
import com.hm.homemaking.dto.MiniOrderSubmitRequest;
import com.hm.homemaking.dto.MiniReviewRequest;
import com.hm.homemaking.dto.OrderExtraSelection;
import com.hm.homemaking.mapper.HmConfigMapper;
import com.hm.homemaking.mapper.HmCustomerMapper;
import com.hm.homemaking.mapper.HmOrderMapper;
import com.hm.homemaking.mapper.HmServiceMapper;
import com.hm.homemaking.mapper.HmWorkerMapper;
import com.hm.homemaking.service.HmAppAuthService;
import com.hm.homemaking.service.HmMiniService;
import com.hm.homemaking.service.WechatMiniClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HmMiniServiceImpl implements HmMiniService
{
    @Autowired
    private HmCustomerMapper customerMapper;

    @Autowired
    private HmServiceMapper serviceMapper;

    @Autowired
    private HmWorkerMapper workerMapper;

    @Autowired
    private HmOrderMapper orderMapper;

    @Autowired
    private HmConfigMapper configMapper;

    @Autowired
    private HmAppAuthService appAuthService;

    @Autowired
    private WechatMiniClient wechatMiniClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(MiniLoginRequest request)
    {
        WechatMiniClient.Code2SessionResult sessionResult = resolveMiniSession(request);
        String openId = sessionResult.getOpenId();
        HmUserAuth auth = customerMapper.selectAuthByTypeAndKey("WECHAT_MINI", openId);
        HmCustomerUser customerUser;
        if (auth == null)
        {
            customerUser = new HmCustomerUser();
            customerUser.setNickname("wx_user_" + openId.substring(Math.max(openId.length() - 4, 0)));
            customerUser.setAvatar("");
            customerUser.setGender("0");
            customerUser.setStatus("0");
            customerUser.setSourceChannel("WECHAT_MINI");
            customerUser.setLastLoginIp(IpUtils.getIpAddr());
            customerUser.setLastLoginTime(new Date());
            customerUser.setCreateBy("mini");
            customerMapper.insertCustomer(customerUser);

            auth = new HmUserAuth();
            auth.setCustomerId(customerUser.getCustomerId());
            auth.setAuthType("WECHAT_MINI");
            auth.setAuthKey(openId);
            auth.setAuthSecret(sessionResult.getSessionKey());
            auth.setUnionid(sessionResult.getUnionId());
            auth.setStatus("0");
            customerMapper.insertAuth(auth);
        }
        else
        {
            customerUser = customerMapper.selectCustomerById(auth.getCustomerId());
            auth.setAuthSecret(sessionResult.getSessionKey());
            auth.setUnionid(sessionResult.getUnionId());
            customerMapper.updateAuth(auth);
        }

        customerUser.setLastLoginIp(IpUtils.getIpAddr());
        customerUser.setLastLoginTime(new Date());
        customerMapper.updateCustomer(customerUser);

        String token = appAuthService.createToken(customerUser, openId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("customerId", customerUser.getCustomerId());
        result.put("isBindMobile", StringUtils.isNotEmpty(customerUser.getMobile()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindMobile(MiniBindMobileRequest request)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        String mobile = resolvePhoneNumber(request);
        if (StringUtils.isEmpty(mobile))
        {
            throw new ServiceException("手机号不能为空");
        }
        customerUser.setMobile(mobile);
        customerMapper.updateCustomer(customerUser);

        HmUserAuth auth = customerMapper.selectAuthByTypeAndKey("WECHAT_MINI", appAuthService.getLoginUser().getOpenId());
        if (auth != null)
        {
            auth.setMobile(mobile);
            customerMapper.updateAuth(auth);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mobile", mobile);
        result.put("isBindMobile", true);
        return result;
    }

    @Override
    public Map<String, Object> getCurrentProfile()
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerId", customerUser.getCustomerId());
        result.put("nickname", customerUser.getNickname());
        result.put("avatar", customerUser.getAvatar());
        result.put("mobile", customerUser.getMobile());
        result.put("isBindMobile", StringUtils.isNotEmpty(customerUser.getMobile()));
        return result;
    }

    @Override
    public Map<String, Object> getHomeIndex()
    {
        Map<String, String> configMap = configMapper.selectHomeConfigList().stream()
            .collect(Collectors.toMap(item -> item.getConfigKey(), item -> item.getConfigValue(), (left, right) -> right, LinkedHashMap::new));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bannerList", parseJsonArray(configMap.get("bannerList")));
        result.put("advantageList", parseJsonArray(configMap.get("advantageList")));
        result.put("serviceNavList", parseJsonArray(configMap.get("serviceNavList")));
        result.put("promoCardList", parseJsonArray(configMap.get("promoCardList")));
        result.put("searchConfig", parseJsonObject(configMap.get("searchConfig")));
        result.put("sectionConfig", parseJsonObject(configMap.get("sectionConfig")));
        result.put("categoryBannerList", parseJsonArray(configMap.get("categoryBannerList")));
        result.put("categorySectionList", parseJsonArray(configMap.get("categorySectionList")));
        result.put("mineMenuList", parseJsonArray(configMap.get("mineMenuList")));
        result.put("contactConfig", parseJsonObject(configMap.get("contactConfig")));
        result.put("categoryList", serviceMapper.selectMiniCategoryList());
        result.put("hotServiceList", serviceMapper.selectMiniHotServiceList());
        result.put("recommendWorkerList", workerMapper.selectRecommendWorkerList());
        result.put("navList", getNavConfig());
        return result;
    }

    @Override
    public Map<String, Object> getContentByType(String contentType)
    {
        String targetType = StringUtils.trim(contentType);
        if (StringUtils.isEmpty(targetType))
        {
            throw new ServiceException("content type is required");
        }
        HmPortalContent content = configMapper.selectMiniContentByType(targetType);
        if (content == null)
        {
            throw new ServiceException("content is unavailable");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contentId", content.getContentId());
        result.put("contentType", content.getContentType());
        result.put("title", content.getTitle());
        result.put("subTitle", content.getSubTitle());
        result.put("coverImage", content.getCoverImage());
        result.put("summary", content.getSummary());
        result.put("contentHtml", StringUtils.defaultString(content.getContentHtml()));
        result.put("publishTime", content.getPublishTime() == null ? "" : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, content.getPublishTime()));
        result.put("updateTime", content.getUpdateTime() == null ? "" : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, content.getUpdateTime()));
        return result;
    }

    @Override
    public Map<String, Object> getServiceList(Long categoryId, String keyword, Integer pageNum, Integer pageSize)
    {
        HmServiceItem query = new HmServiceItem();
        query.setCategoryId(categoryId);
        query.setServiceName(keyword);
        query.setStatus("0");
        List<HmServiceItem> fullList = serviceMapper.selectServiceList(query).stream()
            .filter(item -> "1".equals(item.getShowInMiniapp()))
            .collect(Collectors.toList());
        return paginate(fullList, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> getServiceDetail(Long serviceItemId)
    {
        HmServiceItem serviceItem = loadServiceDetail(serviceItemId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceInfo", serviceItem);
        result.put("skuList", serviceItem.getSkuList());
        result.put("extraItemList", serviceItem.getExtraItemList());
        result.put("bookingRule", serviceItem.getBookingRule());
        result.put("reviewSummary", null);
        return result;
    }

    @Override
    public List<HmCustomerAddress> listAddresses()
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        return customerMapper.selectAddressList(customerUser.getCustomerId());
    }

    @Override
    public HmCustomerAddress getAddress(Long addressId)
    {
        HmCustomerAddress address = customerMapper.selectAddressById(addressId);
        validateAddressOwnership(address);
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveAddress(HmCustomerAddress address)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        address.setCustomerId(customerUser.getCustomerId());
        if (StringUtils.isEmpty(address.getIsDefault()))
        {
            address.setIsDefault("0");
        }
        if ("1".equals(address.getIsDefault()))
        {
            customerMapper.clearDefaultAddress(customerUser.getCustomerId());
        }
        if (address.getAddressId() == null)
        {
            return customerMapper.insertAddress(address);
        }
        validateAddressOwnership(customerMapper.selectAddressById(address.getAddressId()));
        return customerMapper.updateAddress(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAddress(Long addressId)
    {
        validateAddressOwnership(customerMapper.selectAddressById(addressId));
        return customerMapper.deleteAddress(addressId);
    }

    @Override
    public Map<String, Object> preCalc(MiniOrderCalcRequest request)
    {
        Map<String, Object> calc = buildOrderCalc(request);
        calc.remove("_serviceItem");
        calc.remove("_address");
        calc.remove("_sku");
        return calc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitOrder(MiniOrderSubmitRequest request)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        Map<String, Object> calc = buildOrderCalc(request);
        HmServiceItem serviceItem = (HmServiceItem) calc.get("_serviceItem");
        HmCustomerAddress address = (HmCustomerAddress) calc.get("_address");
        HmServiceSku sku = (HmServiceSku) calc.get("_sku");

        HmOrder order = new HmOrder();
        order.setOrderNo(buildOrderNo());
        order.setCustomerId(customerUser.getCustomerId());
        order.setOrgId(serviceItem.getOrgId());
        order.setServiceItemId(serviceItem.getServiceItemId());
        order.setSkuId(sku == null ? null : sku.getSkuId());
        order.setOrderType("service");
        order.setOrderStatus("1".equals(serviceItem.getNeedManualConfirm()) ? "20" : "30");
        order.setPayStatus("0");
        order.setAssignStatus("0");
        order.setContactName(address.getContactName());
        order.setContactMobile(address.getContactMobile());
        order.setServiceAddress(buildAddressText(address));
        order.setProvinceCode(address.getProvinceCode());
        order.setCityCode(address.getCityCode());
        order.setDistrictCode(address.getDistrictCode());
        order.setAppointmentDate(request.getAppointmentDate());
        order.setAppointmentTimeSlot(request.getAppointmentTimeSlot());
        order.setBaseAmount((BigDecimal) calc.get("baseAmount"));
        order.setExtraAmount((BigDecimal) calc.get("extraAmount"));
        order.setDiscountAmount((BigDecimal) calc.get("discountAmount"));
        order.setPayAmount((BigDecimal) calc.get("payAmount"));
        order.setSourceChannel("WECHAT_MINI");
        order.setCustomerRemark(request.getCustomerRemark());
        orderMapper.insertOrder(order);

        HmOrderItem mainItem = new HmOrderItem();
        mainItem.setOrderId(order.getOrderId());
        mainItem.setServiceItemId(serviceItem.getServiceItemId());
        mainItem.setSkuId(sku == null ? null : sku.getSkuId());
        mainItem.setItemName(sku == null ? serviceItem.getServiceName() : sku.getSkuName());
        mainItem.setItemPrice(sku == null ? serviceItem.getBasePrice() : sku.getPrice());
        mainItem.setQuantity(1);
        mainItem.setItemAmount(mainItem.getItemPrice());
        orderMapper.insertOrderItem(mainItem);

        for (HmServiceExtraItem extraItem : resolveSelectedExtras(serviceItem, request.getExtraItemList()))
        {
            HmOrderItem item = new HmOrderItem();
            item.setOrderId(order.getOrderId());
            item.setServiceItemId(serviceItem.getServiceItemId());
            item.setItemName(extraItem.getExtraName());
            item.setItemPrice(extraItem.getExtraPrice());
            item.setQuantity(extraItem.getQuantity());
            item.setItemAmount(extraItem.getExtraPrice().multiply(BigDecimal.valueOf(extraItem.getQuantity())));
            orderMapper.insertOrderItem(item);
        }

        insertOrderLog(order.getOrderId(), "customer", customerUser.getCustomerId(), customerUser.getNickname(), "CREATE", "submit order");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", order.getOrderNo());
        result.put("payAmount", order.getPayAmount());
        result.put("orderStatus", order.getOrderStatus());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payOrder(Long orderId)
    {
        throw new ServiceException("支付功能已调整为后期开发，当前版本暂未启用");
    }

    @Override
    public Map<String, Object> listOrders(String orderStatus, Integer pageNum, Integer pageSize)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        HmOrder query = new HmOrder();
        query.setCustomerId(customerUser.getCustomerId());
        query.setOrderStatus(orderStatus);
        List<HmOrder> fullList = orderMapper.selectOrderList(query);
        return paginate(fullList, pageNum, pageSize);
    }

    @Override
    public HmOrder getOrderDetail(Long orderId)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        HmOrder order = requireOwnOrder(orderId, customerUser.getCustomerId());
        order.setItemList(orderMapper.selectOrderItems(orderId));
        order.setOperateLogs(orderMapper.selectOrderLogs(orderId));
        order.setAssignInfo(orderMapper.selectLatestAssign(orderId));
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(Long orderId, String cancelReason)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        HmOrder order = requireOwnOrder(orderId, customerUser.getCustomerId());
        order.setOrderStatus("80");
        order.setCancelReason(cancelReason);
        orderMapper.updateOrder(order);
        insertOrderLog(orderId, "customer", customerUser.getCustomerId(), customerUser.getNickname(), "CANCEL", StringUtils.defaultIfBlank(cancelReason, "cancel by customer"));
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitReview(MiniReviewRequest request)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        HmOrder order = requireOwnOrder(request.getOrderId(), customerUser.getCustomerId());
        HmReview review = new HmReview();
        review.setOrderId(order.getOrderId());
        review.setCustomerId(customerUser.getCustomerId());
        HmOrderAssign assign = orderMapper.selectLatestAssign(order.getOrderId());
        review.setWorkerId(assign == null ? null : assign.getWorkerId());
        review.setScore(request.getScore());
        review.setTagsJson(JSON.toJSONString(request.getTags()));
        review.setContent(request.getContent());
        review.setImagesJson(JSON.toJSONString(request.getImages()));
        review.setIsAnonymous(StringUtils.defaultIfBlank(request.getIsAnonymous(), "0"));
        review.setStatus("0");
        orderMapper.insertReview(review);

        order.setOrderStatus("70");
        orderMapper.updateOrder(order);
        insertOrderLog(order.getOrderId(), "customer", customerUser.getCustomerId(), customerUser.getNickname(), "REVIEW", "submit review");
        return 1;
    }

    @Override
    public List<Map<String, Object>> getNavConfig()
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (HmMiniNavConfig config : configMapper.selectNavConfigList())
        {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("navId", config.getNavId());
            item.put("navName", config.getNavName());
            item.put("navIcon", config.getNavIcon());
            item.put("navPath", config.getNavPath());
            item.put("sortNo", config.getSortNo());
            result.add(item);
        }
        return result;
    }

    private HmServiceItem loadServiceDetail(Long serviceItemId)
    {
        HmServiceItem serviceItem = serviceMapper.selectServiceById(serviceItemId);
        if (serviceItem == null || !"0".equals(serviceItem.getStatus()))
        {
            throw new ServiceException("service item is unavailable");
        }
        serviceItem.setSkuList(serviceMapper.selectSkuListByServiceItemId(serviceItemId));
        serviceItem.setExtraItemList(serviceMapper.selectExtraItemListByServiceItemId(serviceItemId));
        serviceItem.setAreaIdList(serviceMapper.selectAreaIdsByServiceItemId(serviceItemId));
        serviceItem.setBookingRule(serviceMapper.selectBookingRuleByServiceItemId(serviceItemId));
        return serviceItem;
    }

    private void validateAddressOwnership(HmCustomerAddress address)
    {
        HmCustomerUser customerUser = appAuthService.getCurrentCustomer();
        if (address == null || !customerUser.getCustomerId().equals(address.getCustomerId()))
        {
            throw new ServiceException("address is unavailable");
        }
    }

    private Map<String, Object> buildOrderCalc(MiniOrderCalcRequest request)
    {
        HmServiceItem serviceItem = loadServiceDetail(request.getServiceItemId());
        HmCustomerAddress address = customerMapper.selectAddressById(request.getAddressId());
        validateAddressOwnership(address);
        HmServiceSku sku = resolveSku(serviceItem, request.getSkuId());
        HmServiceArea matchedArea = resolveServiceArea(serviceItem, address);
        HmBookingRule bookingRule = serviceItem.getBookingRule();
        validateTimeSlot(bookingRule, request.getAppointmentTimeSlot());

        BigDecimal baseAmount = sku == null ? serviceItem.getBasePrice() : sku.getPrice();
        BigDecimal extraAmount = BigDecimal.ZERO;
        for (HmServiceExtraItem extraItem : resolveSelectedExtras(serviceItem, request.getExtraItemList()))
        {
            extraAmount = extraAmount.add(extraItem.getExtraPrice().multiply(BigDecimal.valueOf(extraItem.getQuantity())));
        }
        if (matchedArea != null && matchedArea.getExtraFee() != null)
        {
            extraAmount = extraAmount.add(matchedArea.getExtraFee());
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = baseAmount.add(extraAmount).subtract(discountAmount);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseAmount", baseAmount);
        result.put("extraAmount", extraAmount);
        result.put("discountAmount", discountAmount);
        result.put("payAmount", payAmount);
        result.put("_serviceItem", serviceItem);
        result.put("_address", address);
        result.put("_sku", sku);
        return result;
    }

    private HmServiceSku resolveSku(HmServiceItem serviceItem, Long skuId)
    {
        List<HmServiceSku> skuList = serviceItem.getSkuList();
        if (skuList == null || skuList.isEmpty())
        {
            return null;
        }
        if (skuId == null)
        {
            return skuList.get(0);
        }
        return skuList.stream()
            .filter(item -> skuId.equals(item.getSkuId()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("sku is unavailable"));
    }

    private HmServiceArea resolveServiceArea(HmServiceItem serviceItem, HmCustomerAddress address)
    {
        if (serviceItem.getAreaIdList() == null || serviceItem.getAreaIdList().isEmpty())
        {
            return null;
        }
        for (Long areaId : serviceItem.getAreaIdList())
        {
            HmServiceArea area = serviceMapper.selectAreaById(areaId);
            if (area != null && (
                StringUtils.equals(area.getDistrictName(), address.getDistrictName()) ||
                StringUtils.equals(area.getAreaName(), address.getDistrictName()) ||
                StringUtils.equals(area.getAreaName(), address.getCityName())))
            {
                return area;
            }
        }
        throw new ServiceException("current address is out of service area");
    }

    private void validateTimeSlot(HmBookingRule bookingRule, String appointmentTimeSlot)
    {
        if (bookingRule == null || StringUtils.isEmpty(appointmentTimeSlot) || StringUtils.isEmpty(bookingRule.getTimeSlotsJson()))
        {
            return;
        }
        List<String> slots = JSON.parseArray(bookingRule.getTimeSlotsJson(), String.class);
        if (slots != null && !slots.isEmpty() && !slots.contains(appointmentTimeSlot))
        {
            throw new ServiceException("appointment time slot is unavailable");
        }
    }

    private List<HmServiceExtraItem> resolveSelectedExtras(HmServiceItem serviceItem, List<OrderExtraSelection> selections)
    {
        if (selections == null || selections.isEmpty() || serviceItem.getExtraItemList() == null || serviceItem.getExtraItemList().isEmpty())
        {
            return Collections.emptyList();
        }
        Map<Long, HmServiceExtraItem> extraMap = serviceItem.getExtraItemList().stream()
            .collect(Collectors.toMap(HmServiceExtraItem::getExtraItemId, item -> item));
        List<HmServiceExtraItem> result = new ArrayList<>();
        for (OrderExtraSelection selection : selections)
        {
            HmServiceExtraItem extraItem = extraMap.get(selection.getExtraItemId());
            if (extraItem != null)
            {
                HmServiceExtraItem copy = new HmServiceExtraItem();
                copy.setExtraItemId(extraItem.getExtraItemId());
                copy.setExtraName(extraItem.getExtraName());
                copy.setExtraPrice(extraItem.getExtraPrice());
                copy.setQuantity(selection.getQuantity() == null ? 1 : selection.getQuantity());
                result.add(copy);
            }
        }
        return result;
    }

    private String buildOrderNo()
    {
        return "HM" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + ((int) (Math.random() * 9000) + 1000);
    }

    private WechatMiniClient.Code2SessionResult resolveMiniSession(MiniLoginRequest request)
    {
        String code = StringUtils.trim(request.getCode());
        if (StringUtils.isEmpty(code))
        {
            throw new ServiceException("微信登录凭证不能为空");
        }
        return wechatMiniClient.code2Session(code);
    }

    private String resolvePhoneNumber(MiniBindMobileRequest request)
    {
        if (StringUtils.isNotEmpty(request.getPhoneCode()))
        {
            return wechatMiniClient.getPhoneNumber(StringUtils.trim(request.getPhoneCode()));
        }
        String mobile = StringUtils.trim(request.getEncryptedData());
        if (StringUtils.isEmpty(mobile))
        {
            mobile = StringUtils.trim(request.getIv());
        }
        return mobile;
    }

    private String buildAddressText(HmCustomerAddress address)
    {
        return StringUtils.defaultString(address.getProvinceName())
            + StringUtils.defaultString(address.getCityName())
            + StringUtils.defaultString(address.getDistrictName())
            + StringUtils.defaultString(address.getDetailAddress());
    }

    private HmOrder requireOwnOrder(Long orderId, Long customerId)
    {
        HmOrder order = orderMapper.selectOrderById(orderId);
        if (order == null || !customerId.equals(order.getCustomerId()))
        {
            throw new ServiceException("order is unavailable");
        }
        return order;
    }

    private void insertOrderLog(Long orderId, String operatorType, Long operatorId, String operatorName, String actionType, String actionDesc)
    {
        HmOrderOperateLog log = new HmOrderOperateLog();
        log.setOrderId(orderId);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setActionType(actionType);
        log.setActionDesc(actionDesc);
        orderMapper.insertOrderLog(log);
    }

    private List<Object> parseJsonArray(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return Collections.emptyList();
        }
        return JSON.parseArray(value, Object.class);
    }

    private Map<String, Object> parseJsonObject(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return Collections.emptyMap();
        }
        Map<String, Object> result = JSON.parseObject(value);
        return result == null ? Collections.emptyMap() : result;
    }

    private Map<String, Object> paginate(List<?> fullList, Integer pageNum, Integer pageSize)
    {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((currentPage - 1) * currentSize, fullList.size());
        int toIndex = Math.min(fromIndex + currentSize, fullList.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", fullList.subList(fromIndex, toIndex));
        result.put("total", fullList.size());
        result.put("pageNum", currentPage);
        result.put("pageSize", currentSize);
        return result;
    }
}
