package com.hm.homemaking.domain;

import com.hm.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.List;

public class HmServiceItem extends BaseEntity
{
    private Long serviceItemId;
    private Long categoryId;
    private String categoryName;
    private String serviceName;
    private Long orgId;
    private String serviceSubTitle;
    private String serviceCover;
    private String serviceCoverStorage;
    private String serviceCoverObjectKey;
    private String serviceDesc;
    private String serviceContent;
    private Integer serviceDuration;
    private String saleType;
    private BigDecimal basePrice;
    private String unitName;
    private String needManualConfirm;
    private String allowAssignWorker;
    private String showInMiniapp;
    private String showInPortal;
    private String status;
    private String delFlag;
    private HmBookingRule bookingRule;
    private List<HmServiceSku> skuList;
    private List<HmServiceExtraItem> extraItemList;
    private List<Long> areaIdList;

    public Long getServiceItemId()
    {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId)
    {
        this.serviceItemId = serviceItemId;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    public Long getOrgId()
    {
        return orgId;
    }

    public void setOrgId(Long orgId)
    {
        this.orgId = orgId;
    }

    public String getServiceSubTitle()
    {
        return serviceSubTitle;
    }

    public void setServiceSubTitle(String serviceSubTitle)
    {
        this.serviceSubTitle = serviceSubTitle;
    }

    public String getServiceCover()
    {
        return serviceCover;
    }

    public void setServiceCover(String serviceCover)
    {
        this.serviceCover = serviceCover;
    }

    public String getServiceCoverStorage()
    {
        return serviceCoverStorage;
    }

    public void setServiceCoverStorage(String serviceCoverStorage)
    {
        this.serviceCoverStorage = serviceCoverStorage;
    }

    public String getServiceCoverObjectKey()
    {
        return serviceCoverObjectKey;
    }

    public void setServiceCoverObjectKey(String serviceCoverObjectKey)
    {
        this.serviceCoverObjectKey = serviceCoverObjectKey;
    }

    public String getServiceDesc()
    {
        return serviceDesc;
    }

    public void setServiceDesc(String serviceDesc)
    {
        this.serviceDesc = serviceDesc;
    }

    public String getServiceContent()
    {
        return serviceContent;
    }

    public void setServiceContent(String serviceContent)
    {
        this.serviceContent = serviceContent;
    }

    public Integer getServiceDuration()
    {
        return serviceDuration;
    }

    public void setServiceDuration(Integer serviceDuration)
    {
        this.serviceDuration = serviceDuration;
    }

    public String getSaleType()
    {
        return saleType;
    }

    public void setSaleType(String saleType)
    {
        this.saleType = saleType;
    }

    public BigDecimal getBasePrice()
    {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice)
    {
        this.basePrice = basePrice;
    }

    public String getUnitName()
    {
        return unitName;
    }

    public void setUnitName(String unitName)
    {
        this.unitName = unitName;
    }

    public String getNeedManualConfirm()
    {
        return needManualConfirm;
    }

    public void setNeedManualConfirm(String needManualConfirm)
    {
        this.needManualConfirm = needManualConfirm;
    }

    public String getAllowAssignWorker()
    {
        return allowAssignWorker;
    }

    public void setAllowAssignWorker(String allowAssignWorker)
    {
        this.allowAssignWorker = allowAssignWorker;
    }

    public String getShowInMiniapp()
    {
        return showInMiniapp;
    }

    public void setShowInMiniapp(String showInMiniapp)
    {
        this.showInMiniapp = showInMiniapp;
    }

    public String getShowInPortal()
    {
        return showInPortal;
    }

    public void setShowInPortal(String showInPortal)
    {
        this.showInPortal = showInPortal;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public HmBookingRule getBookingRule()
    {
        return bookingRule;
    }

    public void setBookingRule(HmBookingRule bookingRule)
    {
        this.bookingRule = bookingRule;
    }

    public List<HmServiceSku> getSkuList()
    {
        return skuList;
    }

    public void setSkuList(List<HmServiceSku> skuList)
    {
        this.skuList = skuList;
    }

    public List<HmServiceExtraItem> getExtraItemList()
    {
        return extraItemList;
    }

    public void setExtraItemList(List<HmServiceExtraItem> extraItemList)
    {
        this.extraItemList = extraItemList;
    }

    public List<Long> getAreaIdList()
    {
        return areaIdList;
    }

    public void setAreaIdList(List<Long> areaIdList)
    {
        this.areaIdList = areaIdList;
    }
}
