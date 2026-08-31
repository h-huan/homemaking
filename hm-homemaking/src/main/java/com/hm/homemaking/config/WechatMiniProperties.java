package com.hm.homemaking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat.mini")
public class WechatMiniProperties
{
    /**
     * 微信小程序 AppID
     */
    private String appId;

    /**
     * 微信小程序 AppSecret
     */
    private String appSecret;

    /**
     * 是否启用真实微信登录
     */
    private boolean enabled;

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getAppSecret()
    {
        return appSecret;
    }

    public void setAppSecret(String appSecret)
    {
        this.appSecret = appSecret;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
