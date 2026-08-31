package com.hm.homemaking.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class HmOrder
{
    private Long orderId;
    private String orderNo;
    private Long customerId;
    private Long orgId;
    private Long serviceItemId;
    private String serviceName;
    private Long skuId;
    private String skuName;
    private String orderType;
    private String orderStatus;
    private String payStatus;
    private String assignStatus;
    private String contactName;
    private String contactMobile;
    private String serviceAddress;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private Date appointmentDate;
    private String appointmentTimeSlot;
    private Date serviceStartTime;
    private Date serviceEndTime;
    private BigDecimal baseAmount;
    private BigDecimal extraAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Date payTime;
    private String payChannel;
    private String sourceChannel;
    private String customerRemark;
    private String cancelReason;
    private Date createTime;
    private Date updateTime;
    private List<HmOrderItem> itemList;
    private List<HmOrderOperateLog> operateLogs;
    private HmOrderAssign assignInfo;

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public Long getOrgId()
    {
        return orgId;
    }

    public void setOrgId(Long orgId)
    {
        this.orgId = orgId;
    }

    public Long getServiceItemId()
    {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId)
    {
        this.serviceItemId = serviceItemId;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public void setServiceName(String serviceName)
    {
        this.serviceName = serviceName;
    }

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public String getSkuName()
    {
        return skuName;
    }

    public void setSkuName(String skuName)
    {
        this.skuName = skuName;
    }

    public String getOrderType()
    {
        return orderType;
    }

    public void setOrderType(String orderType)
    {
        this.orderType = orderType;
    }

    public String getOrderStatus()
    {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus)
    {
        this.orderStatus = orderStatus;
    }

    public String getPayStatus()
    {
        return payStatus;
    }

    public void setPayStatus(String payStatus)
    {
        this.payStatus = payStatus;
    }

    public String getAssignStatus()
    {
        return assignStatus;
    }

    public void setAssignStatus(String assignStatus)
    {
        this.assignStatus = assignStatus;
    }

    public String getContactName()
    {
        return contactName;
    }

    public void setContactName(String contactName)
    {
        this.contactName = contactName;
    }

    public String getContactMobile()
    {
        return contactMobile;
    }

    public void setContactMobile(String contactMobile)
    {
        this.contactMobile = contactMobile;
    }

    public String getServiceAddress()
    {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress)
    {
        this.serviceAddress = serviceAddress;
    }

    public String getProvinceCode()
    {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode)
    {
        this.provinceCode = provinceCode;
    }

    public String getCityCode()
    {
        return cityCode;
    }

    public void setCityCode(String cityCode)
    {
        this.cityCode = cityCode;
    }

    public String getDistrictCode()
    {
        return districtCode;
    }

    public void setDistrictCode(String districtCode)
    {
        this.districtCode = districtCode;
    }

    public Date getAppointmentDate()
    {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate)
    {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTimeSlot()
    {
        return appointmentTimeSlot;
    }

    public void setAppointmentTimeSlot(String appointmentTimeSlot)
    {
        this.appointmentTimeSlot = appointmentTimeSlot;
    }

    public Date getServiceStartTime()
    {
        return serviceStartTime;
    }

    public void setServiceStartTime(Date serviceStartTime)
    {
        this.serviceStartTime = serviceStartTime;
    }

    public Date getServiceEndTime()
    {
        return serviceEndTime;
    }

    public void setServiceEndTime(Date serviceEndTime)
    {
        this.serviceEndTime = serviceEndTime;
    }

    public BigDecimal getBaseAmount()
    {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount)
    {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getExtraAmount()
    {
        return extraAmount;
    }

    public void setExtraAmount(BigDecimal extraAmount)
    {
        this.extraAmount = extraAmount;
    }

    public BigDecimal getDiscountAmount()
    {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount)
    {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPayAmount()
    {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount)
    {
        this.payAmount = payAmount;
    }

    public Date getPayTime()
    {
        return payTime;
    }

    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public String getPayChannel()
    {
        return payChannel;
    }

    public void setPayChannel(String payChannel)
    {
        this.payChannel = payChannel;
    }

    public String getSourceChannel()
    {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel)
    {
        this.sourceChannel = sourceChannel;
    }

    public String getCustomerRemark()
    {
        return customerRemark;
    }

    public void setCustomerRemark(String customerRemark)
    {
        this.customerRemark = customerRemark;
    }

    public String getCancelReason()
    {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason)
    {
        this.cancelReason = cancelReason;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public List<HmOrderItem> getItemList()
    {
        return itemList;
    }

    public void setItemList(List<HmOrderItem> itemList)
    {
        this.itemList = itemList;
    }

    public List<HmOrderOperateLog> getOperateLogs()
    {
        return operateLogs;
    }

    public void setOperateLogs(List<HmOrderOperateLog> operateLogs)
    {
        this.operateLogs = operateLogs;
    }

    public HmOrderAssign getAssignInfo()
    {
        return assignInfo;
    }

    public void setAssignInfo(HmOrderAssign assignInfo)
    {
        this.assignInfo = assignInfo;
    }
}
