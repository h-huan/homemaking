package com.hm.homemaking.dto;

import java.util.Date;
import java.util.List;

public class MiniOrderCalcRequest
{
    private Long serviceItemId;
    private Long skuId;
    private List<OrderExtraSelection> extraItemList;
    private Long addressId;
    private Date appointmentDate;
    private String appointmentTimeSlot;

    public Long getServiceItemId()
    {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId)
    {
        this.serviceItemId = serviceItemId;
    }

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public List<OrderExtraSelection> getExtraItemList()
    {
        return extraItemList;
    }

    public void setExtraItemList(List<OrderExtraSelection> extraItemList)
    {
        this.extraItemList = extraItemList;
    }

    public Long getAddressId()
    {
        return addressId;
    }

    public void setAddressId(Long addressId)
    {
        this.addressId = addressId;
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
}
