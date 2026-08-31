package com.hm.homemaking.dto;

public class MiniOrderSubmitRequest extends MiniOrderCalcRequest
{
    private String customerRemark;

    public String getCustomerRemark()
    {
        return customerRemark;
    }

    public void setCustomerRemark(String customerRemark)
    {
        this.customerRemark = customerRemark;
    }
}
