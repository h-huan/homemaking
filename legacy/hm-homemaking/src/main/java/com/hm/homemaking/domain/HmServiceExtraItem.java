package com.hm.homemaking.domain;

import java.math.BigDecimal;

public class HmServiceExtraItem
{
    private Long extraItemId;
    private Long serviceItemId;
    private String extraName;
    private BigDecimal extraPrice;
    private String chargeType;
    private Integer sortNo;
    private String status;
    private Integer quantity;

    public Long getExtraItemId()
    {
        return extraItemId;
    }

    public void setExtraItemId(Long extraItemId)
    {
        this.extraItemId = extraItemId;
    }

    public Long getServiceItemId()
    {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId)
    {
        this.serviceItemId = serviceItemId;
    }

    public String getExtraName()
    {
        return extraName;
    }

    public void setExtraName(String extraName)
    {
        this.extraName = extraName;
    }

    public BigDecimal getExtraPrice()
    {
        return extraPrice;
    }

    public void setExtraPrice(BigDecimal extraPrice)
    {
        this.extraPrice = extraPrice;
    }

    public String getChargeType()
    {
        return chargeType;
    }

    public void setChargeType(String chargeType)
    {
        this.chargeType = chargeType;
    }

    public Integer getSortNo()
    {
        return sortNo;
    }

    public void setSortNo(Integer sortNo)
    {
        this.sortNo = sortNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }
}
