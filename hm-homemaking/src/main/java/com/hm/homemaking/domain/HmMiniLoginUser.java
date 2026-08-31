package com.hm.homemaking.domain;

import java.io.Serializable;

public class HmMiniLoginUser implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long customerId;
    private String mobile;
    private String nickname;
    private String avatar;
    private String openId;
    private String token;
    private long loginTime;
    private long expireTime;

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getOpenId()
    {
        return openId;
    }

    public void setOpenId(String openId)
    {
        this.openId = openId;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public long getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(long loginTime)
    {
        this.loginTime = loginTime;
    }

    public long getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(long expireTime)
    {
        this.expireTime = expireTime;
    }
}
