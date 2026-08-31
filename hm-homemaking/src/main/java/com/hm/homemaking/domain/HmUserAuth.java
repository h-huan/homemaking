package com.hm.homemaking.domain;

public class HmUserAuth
{
    private Long authId;
    private Long userId;
    private Long customerId;
    private String authType;
    private String authKey;
    private String authSecret;
    private String unionid;
    private String mobile;
    private String status;

    public Long getAuthId()
    {
        return authId;
    }

    public void setAuthId(Long authId)
    {
        this.authId = authId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getAuthType()
    {
        return authType;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

    public String getAuthKey()
    {
        return authKey;
    }

    public void setAuthKey(String authKey)
    {
        this.authKey = authKey;
    }

    public String getAuthSecret()
    {
        return authSecret;
    }

    public void setAuthSecret(String authSecret)
    {
        this.authSecret = authSecret;
    }

    public String getUnionid()
    {
        return unionid;
    }

    public void setUnionid(String unionid)
    {
        this.unionid = unionid;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
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
