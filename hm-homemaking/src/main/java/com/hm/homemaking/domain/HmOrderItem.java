package com.hm.homemaking.domain;

import java.math.BigDecimal;

public class HmOrderItem
{
    private Long orderItemId;
    private Long orderId;
    private Long serviceItemId;
    private Long skuId;
    private String itemName;
    private BigDecimal itemPrice;
    private Integer quantity;
    private BigDecimal itemAmount;

    public Long getOrderItemId()
    {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId)
    {
        this.orderItemId = orderItemId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

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

    public String getItemName()
    {
        return itemName;
    }

    public void setItemName(String itemName)
    {
        this.itemName = itemName;
    }

    public BigDecimal getItemPrice()
    {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice)
    {
        this.itemPrice = itemPrice;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getItemAmount()
    {
        return itemAmount;
    }

    public void setItemAmount(BigDecimal itemAmount)
    {
        this.itemAmount = itemAmount;
    }
}
