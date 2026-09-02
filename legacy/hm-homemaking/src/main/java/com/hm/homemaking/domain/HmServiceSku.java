package com.hm.homemaking.domain;

import java.math.BigDecimal;

public class HmServiceSku
{
    private Long skuId;
    private Long serviceItemId;
    private String skuName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer durationMinute;
    private Integer sortNo;
    private String status;

    public Long getSkuId()
    {
        return skuId;
    }

    public void setSkuId(Long skuId)
    {
        this.skuId = skuId;
    }

    public Long getServiceItemId()
    {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId)
    {
        this.serviceItemId = serviceItemId;
    }

    public String getSkuName()
    {
        return skuName;
    }

    public void setSkuName(String skuName)
    {
        this.skuName = skuName;
    }

    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getOriginalPrice()
    {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice)
    {
        this.originalPrice = originalPrice;
    }

    public Integer getDurationMinute()
    {
        return durationMinute;
    }

    public void setDurationMinute(Integer durationMinute)
    {
        this.durationMinute = durationMinute;
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
}
