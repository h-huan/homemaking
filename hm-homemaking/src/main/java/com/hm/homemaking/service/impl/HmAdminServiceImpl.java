package com.hm.homemaking.service.impl;

import com.hm.common.core.domain.entity.SysUser;
import com.hm.common.exception.ServiceException;
import com.hm.common.utils.SecurityUtils;
import com.hm.common.utils.StringUtils;
import com.hm.homemaking.domain.HmBookingRule;
import com.hm.homemaking.domain.HmMiniHomeConfig;
import com.hm.homemaking.domain.HmMiniNavConfig;
import com.hm.homemaking.domain.HmOrder;
import com.hm.homemaking.domain.HmOrderAssign;
import com.hm.homemaking.domain.HmOrderOperateLog;
import com.hm.homemaking.domain.HmPortalContent;
import com.hm.homemaking.domain.HmServiceArea;
import com.hm.homemaking.domain.HmServiceCategory;
import com.hm.homemaking.domain.HmServiceExtraItem;
import com.hm.homemaking.domain.HmServiceItem;
import com.hm.homemaking.domain.HmServiceSku;
import com.hm.homemaking.domain.HmWorkerProfile;
import com.hm.homemaking.dto.AdminOrderAssignRequest;
import com.hm.homemaking.dto.AdminOrderCancelRequest;
import com.hm.homemaking.dto.AdminOrderRefundRequest;
import com.hm.homemaking.mapper.HmCategoryMapper;
import com.hm.homemaking.mapper.HmConfigMapper;
import com.hm.homemaking.mapper.HmOrderMapper;
import com.hm.homemaking.mapper.HmServiceMapper;
import com.hm.homemaking.mapper.HmWorkerMapper;
import com.hm.homemaking.service.HmAdminService;
import com.hm.system.mapper.SysUserMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HmAdminServiceImpl implements HmAdminService
{
    private static final Long DEFAULT_DEPT_ID = 103L;

    @Autowired
    private HmCategoryMapper categoryMapper;

    @Autowired
    private HmServiceMapper serviceMapper;

    @Autowired
    private HmWorkerMapper workerMapper;

    @Autowired
    private HmOrderMapper orderMapper;

    @Autowired
    private HmConfigMapper configMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<HmServiceCategory> listCategories(HmServiceCategory query)
    {
        return categoryMapper.selectCategoryList(query);
    }

    @Override
    public HmServiceCategory getCategory(Long categoryId)
    {
        return categoryMapper.selectCategoryById(categoryId);
    }

    @Override
    public int saveCategory(HmServiceCategory category, String operator)
    {
        fillCategoryDefaults(category);
        if (category.getCategoryId() == null)
        {
            category.setCreateBy(operator);
            return categoryMapper.insertCategory(category);
        }
        category.setUpdateBy(operator);
        return categoryMapper.updateCategory(category);
    }

    @Override
    public int removeCategory(Long categoryId)
    {
        return categoryMapper.deleteCategoryById(categoryId);
    }

    @Override
    public List<HmServiceArea> listAreas(HmServiceArea query)
    {
        return serviceMapper.selectAreaList(query);
    }

    @Override
    public HmServiceArea getArea(Long areaId)
    {
        return serviceMapper.selectAreaById(areaId);
    }

    @Override
    public int saveArea(HmServiceArea area)
    {
        if (StringUtils.isEmpty(area.getStatus()))
        {
            area.setStatus("0");
        }
        if (area.getExtraFee() == null)
        {
            area.setExtraFee(BigDecimal.ZERO);
        }
        if (area.getAreaId() == null)
        {
            return serviceMapper.insertArea(area);
        }
        return serviceMapper.updateArea(area);
    }

    @Override
    public List<HmServiceItem> listServices(HmServiceItem query)
    {
        return serviceMapper.selectServiceList(query);
    }

    @Override
    public HmServiceItem getService(Long serviceItemId)
    {
        HmServiceItem serviceItem = serviceMapper.selectServiceById(serviceItemId);
        if (serviceItem == null)
        {
            return null;
        }
        serviceItem.setSkuList(serviceMapper.selectSkuListByServiceItemId(serviceItemId));
        serviceItem.setExtraItemList(serviceMapper.selectExtraItemListByServiceItemId(serviceItemId));
        serviceItem.setAreaIdList(serviceMapper.selectAreaIdsByServiceItemId(serviceItemId));
        serviceItem.setBookingRule(serviceMapper.selectBookingRuleByServiceItemId(serviceItemId));
        return serviceItem;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveService(HmServiceItem serviceItem, String operator)
    {
        fillServiceDefaults(serviceItem);
        int rows;
        if (serviceItem.getServiceItemId() == null)
        {
            serviceItem.setCreateBy(operator);
            rows = serviceMapper.insertService(serviceItem);
        }
        else
        {
            serviceItem.setUpdateBy(operator);
            rows = serviceMapper.updateService(serviceItem);
            serviceMapper.deleteSkuByServiceItemId(serviceItem.getServiceItemId());
            serviceMapper.deleteExtraItemByServiceItemId(serviceItem.getServiceItemId());
            serviceMapper.deleteServiceAreaRelByServiceItemId(serviceItem.getServiceItemId());
            serviceMapper.deleteBookingRuleByServiceItemId(serviceItem.getServiceItemId());
        }
        saveServiceChildren(serviceItem);
        return rows;
    }

    @Override
    public int removeService(Long serviceItemId)
    {
        return serviceMapper.deleteServiceById(serviceItemId);
    }

    @Override
    public List<HmWorkerProfile> listWorkers(HmWorkerProfile query)
    {
        return workerMapper.selectWorkerList(query);
    }

    @Override
    public HmWorkerProfile getWorker(Long workerId)
    {
        HmWorkerProfile workerProfile = workerMapper.selectWorkerById(workerId);
        if (workerProfile == null)
        {
            return null;
        }
        workerProfile.setServiceItemIdList(workerMapper.selectServiceIdsByWorkerId(workerId));
        workerProfile.setAreaIdList(workerMapper.selectAreaIdsByWorkerId(workerId));
        return workerProfile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveWorker(HmWorkerProfile workerProfile, String operator)
    {
        fillWorkerDefaults(workerProfile);
        SysUser sysUser;
        if (workerProfile.getWorkerProfileId() == null)
        {
            sysUser = buildWorkerUser(workerProfile, null);
            sysUser.setCreateBy(operator);
            sysUserMapper.insertUser(sysUser);
            workerProfile.setUserId(sysUser.getUserId());
            workerProfile.setCreateBy(operator);
            workerMapper.insertWorkerProfile(workerProfile);
        }
        else
        {
            HmWorkerProfile old = workerMapper.selectWorkerById(workerProfile.getWorkerProfileId());
            if (old == null)
            {
                throw new ServiceException("worker is unavailable");
            }
            workerProfile.setUserId(old.getUserId());
            sysUser = buildWorkerUser(workerProfile, old.getUserId());
            sysUser.setUserId(old.getUserId());
            sysUser.setUpdateBy(operator);
            sysUserMapper.updateUser(sysUser);
            workerProfile.setUpdateBy(operator);
            workerMapper.updateWorkerProfile(workerProfile);
            workerMapper.deleteWorkerServices(workerProfile.getWorkerProfileId());
            workerMapper.deleteWorkerAreas(workerProfile.getWorkerProfileId());
        }
        bindWorkerRelations(workerProfile);
        return 1;
    }

    @Override
    public List<HmOrder> listOrders(HmOrder query)
    {
        return orderMapper.selectOrderList(query);
    }

    @Override
    public HmOrder getOrder(Long orderId)
    {
        HmOrder order = orderMapper.selectOrderById(orderId);
        if (order == null)
        {
            return null;
        }
        order.setItemList(orderMapper.selectOrderItems(orderId));
        order.setOperateLogs(orderMapper.selectOrderLogs(orderId));
        order.setAssignInfo(orderMapper.selectLatestAssign(orderId));
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int assignOrder(AdminOrderAssignRequest request, String operator)
    {
        HmOrder order = requireOrder(request.getOrderId());
        HmWorkerProfile worker = workerMapper.selectWorkerById(request.getWorkerId());
        if (worker == null)
        {
            throw new ServiceException("worker is unavailable");
        }
        HmOrderAssign assign = new HmOrderAssign();
        assign.setOrderId(order.getOrderId());
        assign.setWorkerId(request.getWorkerId());
        assign.setAssignStatus("1");
        assign.setAssignTime(new Date());
        assign.setRemark(request.getRemark());
        orderMapper.insertOrderAssign(assign);

        order.setAssignStatus("1");
        order.setOrderStatus("40");
        orderMapper.updateOrder(order);
        insertOrderLog(order.getOrderId(), "admin", SecurityUtils.getUserId(), operator, "ASSIGN", "assign worker");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelOrder(AdminOrderCancelRequest request, String operator)
    {
        HmOrder order = requireOrder(request.getOrderId());
        order.setOrderStatus("80");
        order.setCancelReason(request.getCancelReason());
        orderMapper.updateOrder(order);
        insertOrderLog(order.getOrderId(), "admin", SecurityUtils.getUserId(), operator, "CANCEL", StringUtils.defaultIfBlank(request.getCancelReason(), "cancel by admin"));
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refundOrder(AdminOrderRefundRequest request, String operator)
    {
        HmOrder order = requireOrder(request.getOrderId());
        order.setOrderStatus("90");
        order.setPayStatus("2");
        orderMapper.updateOrder(order);
        insertOrderLog(order.getOrderId(), "admin", SecurityUtils.getUserId(), operator, "REFUND", StringUtils.defaultIfBlank(request.getRefundReason(), "refund by admin"));
        return 1;
    }

    @Override
    public List<HmMiniHomeConfig> listHomeConfigs()
    {
        return configMapper.selectHomeConfigList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveHomeConfigs(List<HmMiniHomeConfig> configs)
    {
        int rows = 0;
        for (HmMiniHomeConfig config : configs)
        {
            if (StringUtils.isEmpty(config.getStatus()))
            {
                config.setStatus("0");
            }
            rows += configMapper.upsertHomeConfig(config);
        }
        return rows;
    }

    @Override
    public List<HmMiniNavConfig> listNavConfigs()
    {
        return configMapper.selectNavConfigList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveNavConfigs(List<HmMiniNavConfig> configs)
    {
        configMapper.deleteAllNavConfig();
        int rows = 0;
        for (HmMiniNavConfig config : configs)
        {
            if (StringUtils.isEmpty(config.getStatus()))
            {
                config.setStatus("0");
            }
            rows += configMapper.insertNavConfig(config);
        }
        return rows;
    }

    @Override
    public List<HmPortalContent> listPortalContents(HmPortalContent query)
    {
        return configMapper.selectPortalContentList(query);
    }

    @Override
    public HmPortalContent getPortalContent(Long contentId)
    {
        return configMapper.selectPortalContentById(contentId);
    }

    @Override
    public int savePortalContent(HmPortalContent content)
    {
        fillPortalContentDefaults(content);
        validatePortalContent(content);
        content.setUpdateTime(new Date());
        if ("1".equals(content.getPublishStatus()) && content.getPublishTime() == null)
        {
            content.setPublishTime(new Date());
        }
        if (!"1".equals(content.getPublishStatus()))
        {
            content.setPublishTime(null);
        }
        if (content.getContentId() == null)
        {
            return configMapper.insertPortalContent(content);
        }
        return configMapper.updatePortalContent(content);
    }

    @Override
    public int removePortalContent(Long contentId)
    {
        return configMapper.deletePortalContentById(contentId);
    }

    private void fillCategoryDefaults(HmServiceCategory category)
    {
        if (category.getParentId() == null)
        {
            category.setParentId(0L);
        }
        if (category.getSortNo() == null)
        {
            category.setSortNo(0);
        }
        if (StringUtils.isEmpty(category.getShowInMiniapp()))
        {
            category.setShowInMiniapp("1");
        }
        if (StringUtils.isEmpty(category.getShowInPortal()))
        {
            category.setShowInPortal("1");
        }
        if (StringUtils.isEmpty(category.getStatus()))
        {
            category.setStatus("0");
        }
        if (StringUtils.isEmpty(category.getBannerImageStorage()))
        {
            category.setBannerImageStorage("local");
        }
        category.setCategoryIcon(StringUtils.trim(category.getCategoryIcon()));
        category.setBannerImage(StringUtils.trim(category.getBannerImage()));
        category.setBannerImageObjectKey(StringUtils.trim(category.getBannerImageObjectKey()));
    }

    private void fillServiceDefaults(HmServiceItem serviceItem)
    {
        if (StringUtils.isEmpty(serviceItem.getSaleType()))
        {
            serviceItem.setSaleType("fixed");
        }
        if (serviceItem.getBasePrice() == null)
        {
            serviceItem.setBasePrice(BigDecimal.ZERO);
        }
        if (StringUtils.isEmpty(serviceItem.getShowInMiniapp()))
        {
            serviceItem.setShowInMiniapp("1");
        }
        if (StringUtils.isEmpty(serviceItem.getShowInPortal()))
        {
            serviceItem.setShowInPortal("1");
        }
        if (StringUtils.isEmpty(serviceItem.getNeedManualConfirm()))
        {
            serviceItem.setNeedManualConfirm("0");
        }
        if (StringUtils.isEmpty(serviceItem.getAllowAssignWorker()))
        {
            serviceItem.setAllowAssignWorker("0");
        }
        if (StringUtils.isEmpty(serviceItem.getStatus()))
        {
            serviceItem.setStatus("0");
        }
        if (StringUtils.isEmpty(serviceItem.getServiceCoverStorage()))
        {
            serviceItem.setServiceCoverStorage("local");
        }
        serviceItem.setServiceCover(StringUtils.trim(serviceItem.getServiceCover()));
        serviceItem.setServiceCoverObjectKey(StringUtils.trim(serviceItem.getServiceCoverObjectKey()));
    }

    private void fillWorkerDefaults(HmWorkerProfile workerProfile)
    {
        if (workerProfile.getServiceStar() == null)
        {
            workerProfile.setServiceStar(BigDecimal.valueOf(5));
        }
        if (workerProfile.getServiceCount() == null)
        {
            workerProfile.setServiceCount(0);
        }
        if (StringUtils.isEmpty(workerProfile.getEmploymentStatus()))
        {
            workerProfile.setEmploymentStatus("0");
        }
        if (StringUtils.isEmpty(workerProfile.getWorkStatus()))
        {
            workerProfile.setWorkStatus("0");
        }
        if (StringUtils.isEmpty(workerProfile.getStatus()))
        {
            workerProfile.setStatus("0");
        }
    }

    private void fillPortalContentDefaults(HmPortalContent content)
    {
        if (content.getSortNo() == null)
        {
            content.setSortNo(0);
        }
        if (StringUtils.isEmpty(content.getPublishStatus()))
        {
            content.setPublishStatus("0");
        }
        content.setContentType(StringUtils.trim(content.getContentType()));
        content.setTitle(StringUtils.trim(content.getTitle()));
        content.setSubTitle(StringUtils.trim(content.getSubTitle()));
        content.setCoverImage(StringUtils.trim(content.getCoverImage()));
        if (StringUtils.isEmpty(content.getCoverImageStorage()))
        {
            content.setCoverImageStorage("local");
        }
        content.setCoverImageObjectKey(StringUtils.trim(content.getCoverImageObjectKey()));
        content.setSummary(StringUtils.trim(content.getSummary()));
        content.setContentHtml(StringUtils.defaultString(content.getContentHtml()));
    }

    private void validatePortalContent(HmPortalContent content)
    {
        if (StringUtils.isEmpty(content.getContentType()))
        {
            throw new ServiceException("contentType is required");
        }
        if (StringUtils.isEmpty(content.getTitle()))
        {
            throw new ServiceException("title is required");
        }
        HmPortalContent query = new HmPortalContent();
        query.setContentType(content.getContentType());
        List<HmPortalContent> existsList = configMapper.selectPortalContentList(query);
        for (HmPortalContent exists : existsList)
        {
            if (!exists.getContentId().equals(content.getContentId()))
            {
                throw new ServiceException("contentType already exists");
            }
        }
    }

    private void saveServiceChildren(HmServiceItem serviceItem)
    {
        Long serviceItemId = serviceItem.getServiceItemId();
        for (HmServiceSku sku : safeList(serviceItem.getSkuList()))
        {
            sku.setServiceItemId(serviceItemId);
            if (StringUtils.isEmpty(sku.getStatus()))
            {
                sku.setStatus("0");
            }
            if (sku.getSortNo() == null)
            {
                sku.setSortNo(0);
            }
            serviceMapper.insertSku(sku);
        }
        for (HmServiceExtraItem extraItem : safeList(serviceItem.getExtraItemList()))
        {
            extraItem.setServiceItemId(serviceItemId);
            if (StringUtils.isEmpty(extraItem.getChargeType()))
            {
                extraItem.setChargeType("once");
            }
            if (StringUtils.isEmpty(extraItem.getStatus()))
            {
                extraItem.setStatus("0");
            }
            if (extraItem.getSortNo() == null)
            {
                extraItem.setSortNo(0);
            }
            serviceMapper.insertExtraItem(extraItem);
        }
        for (Long areaId : safeList(serviceItem.getAreaIdList()))
        {
            serviceMapper.insertServiceAreaRel(serviceItemId, areaId);
        }

        HmBookingRule bookingRule = serviceItem.getBookingRule();
        if (bookingRule == null)
        {
            bookingRule = new HmBookingRule();
            bookingRule.setAdvanceDays(0);
            bookingRule.setMinAdvanceMinutes(120);
            bookingRule.setMaxAdvanceDays(7);
            bookingRule.setAllowSameDay("1");
            bookingRule.setTimeSlotsJson("[]");
            bookingRule.setCancelRuleJson("{}");
            bookingRule.setStatus("0");
        }
        bookingRule.setServiceItemId(serviceItemId);
        serviceMapper.insertBookingRule(bookingRule);
    }

    private SysUser buildWorkerUser(HmWorkerProfile workerProfile, Long existingUserId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setDeptId(DEFAULT_DEPT_ID);
        sysUser.setUserName(existingUserId == null ? buildWorkerUserName(workerProfile.getMobile()) : null);
        sysUser.setNickName(workerProfile.getWorkerName());
        sysUser.setPhonenumber(workerProfile.getMobile());
        sysUser.setSex(workerProfile.getGender());
        sysUser.setAvatar(workerProfile.getAvatar());
        sysUser.setStatus(workerProfile.getStatus());
        if (existingUserId == null)
        {
            sysUser.setPassword(SecurityUtils.encryptPassword("123456"));
        }
        return sysUser;
    }

    private String buildWorkerUserName(String mobile)
    {
        String base = "worker_" + StringUtils.defaultIfBlank(mobile, String.valueOf(System.currentTimeMillis()));
        SysUser existing = sysUserMapper.checkUserNameUnique(base);
        if (existing == null)
        {
            return base;
        }
        return base + "_" + System.currentTimeMillis();
    }

    private void bindWorkerRelations(HmWorkerProfile workerProfile)
    {
        for (Long serviceItemId : safeList(workerProfile.getServiceItemIdList()))
        {
            workerMapper.insertWorkerService(workerProfile.getWorkerProfileId(), serviceItemId);
        }
        for (Long areaId : safeList(workerProfile.getAreaIdList()))
        {
            workerMapper.insertWorkerArea(workerProfile.getWorkerProfileId(), areaId);
        }
    }

    private HmOrder requireOrder(Long orderId)
    {
        HmOrder order = orderMapper.selectOrderById(orderId);
        if (order == null)
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

    private <T> List<T> safeList(List<T> data)
    {
        return data == null ? new ArrayList<>() : data;
    }
}
