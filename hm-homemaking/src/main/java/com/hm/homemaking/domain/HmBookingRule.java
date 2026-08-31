package com.hm.homemaking.domain;

public class HmBookingRule
{
    private Long ruleId;
    private Long serviceItemId;
    private Integer advanceDays;
    private Integer minAdvanceMinutes;
    private Integer maxAdvanceDays;
    private String allowSameDay;
    private String timeSlotsJson;
    private String cancelRuleJson;
    private String status;

    public Long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId(Long ruleId)
    {
        this.ruleId = ruleId;
    }

    public Long getServiceItemId()
    {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId)
    {
        this.serviceItemId = serviceItemId;
    }

    public Integer getAdvanceDays()
    {
        return advanceDays;
    }

    public void setAdvanceDays(Integer advanceDays)
    {
        this.advanceDays = advanceDays;
    }

    public Integer getMinAdvanceMinutes()
    {
        return minAdvanceMinutes;
    }

    public void setMinAdvanceMinutes(Integer minAdvanceMinutes)
    {
        this.minAdvanceMinutes = minAdvanceMinutes;
    }

    public Integer getMaxAdvanceDays()
    {
        return maxAdvanceDays;
    }

    public void setMaxAdvanceDays(Integer maxAdvanceDays)
    {
        this.maxAdvanceDays = maxAdvanceDays;
    }

    public String getAllowSameDay()
    {
        return allowSameDay;
    }

    public void setAllowSameDay(String allowSameDay)
    {
        this.allowSameDay = allowSameDay;
    }

    public String getTimeSlotsJson()
    {
        return timeSlotsJson;
    }

    public void setTimeSlotsJson(String timeSlotsJson)
    {
        this.timeSlotsJson = timeSlotsJson;
    }

    public String getCancelRuleJson()
    {
        return cancelRuleJson;
    }

    public void setCancelRuleJson(String cancelRuleJson)
    {
        this.cancelRuleJson = cancelRuleJson;
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
