package com.hm.homemaking.dto;

public class OrderExtraSelection
{
    private Long extraItemId;
    private Integer quantity;

    public Long getExtraItemId()
    {
        return extraItemId;
    }

    public void setExtraItemId(Long extraItemId)
    {
        this.extraItemId = extraItemId;
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
