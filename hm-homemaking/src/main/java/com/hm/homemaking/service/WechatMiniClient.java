package com.hm.homemaking.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hm.common.core.redis.RedisCache;
import com.hm.common.exception.ServiceException;
import com.hm.common.utils.StringUtils;
import com.hm.homemaking.config.WechatMiniProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WechatMiniClient
{
    private static final String ACCESS_TOKEN_CACHE_KEY = "wechat:mini:access_token";

    @Autowired
    private WechatMiniProperties wechatMiniProperties;

    @Autowired
    private RedisCache redisCache;

    public Code2SessionResult code2Session(String code)
    {
        validateEnabled();
        String url = "https://api.weixin.qq.com/sns/jscode2session?appid="
            + encode(wechatMiniProperties.getAppId())
            + "&secret=" + encode(wechatMiniProperties.getAppSecret())
            + "&js_code=" + encode(code)
            + "&grant_type=authorization_code";
        JSONObject response = doGet(url);
        validateWechatResponse(response, "微信登录失败");

        Code2SessionResult result = new Code2SessionResult();
        result.setOpenId(response.getString("openid"));
        result.setSessionKey(response.getString("session_key"));
        result.setUnionId(response.getString("unionid"));
        if (StringUtils.isEmpty(result.getOpenId()))
        {
            throw new ServiceException("微信登录失败，请稍后重试");
        }
        return result;
    }

    public String getPhoneNumber(String phoneCode)
    {
        validateEnabled();
        if (StringUtils.isEmpty(phoneCode))
        {
            throw new ServiceException("手机号授权凭证不能为空");
        }

        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + encode(getAccessToken());
        JSONObject requestBody = new JSONObject();
        requestBody.put("code", phoneCode);
        JSONObject response = doPostJson(url, requestBody.toJSONString());
        validateWechatResponse(response, "获取微信手机号失败");

        JSONObject phoneInfo = response.getJSONObject("phone_info");
        if (phoneInfo == null)
        {
            phoneInfo = response.getJSONObject("phoneInfo");
        }
        if (phoneInfo == null)
        {
            throw new ServiceException("微信手机号数据为空");
        }

        String phoneNumber = phoneInfo.getString("purePhoneNumber");
        if (StringUtils.isEmpty(phoneNumber))
        {
            phoneNumber = phoneInfo.getString("phoneNumber");
        }
        if (StringUtils.isEmpty(phoneNumber))
        {
            throw new ServiceException("微信手机号为空");
        }
        return phoneNumber;
    }

    private String getAccessToken()
    {
        String cachedToken = redisCache.getCacheObject(ACCESS_TOKEN_CACHE_KEY);
        if (StringUtils.isNotEmpty(cachedToken))
        {
            return cachedToken;
        }

        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
            + encode(wechatMiniProperties.getAppId())
            + "&secret=" + encode(wechatMiniProperties.getAppSecret());
        JSONObject response = doGet(url);
        validateWechatResponse(response, "获取微信 access_token 失败");

        String accessToken = response.getString("access_token");
        if (StringUtils.isEmpty(accessToken))
        {
            throw new ServiceException("微信 access_token 为空");
        }
        int expiresIn = response.getIntValue("expires_in");
        int ttl = expiresIn > 300 ? expiresIn - 300 : 3600;
        redisCache.setCacheObject(ACCESS_TOKEN_CACHE_KEY, accessToken, ttl, TimeUnit.SECONDS);
        return accessToken;
    }

    private void validateEnabled()
    {
        if (!wechatMiniProperties.isEnabled())
        {
            throw new ServiceException("微信小程序登录未启用，请检查后端 wechat.mini.enabled 配置");
        }
        if (StringUtils.isEmpty(wechatMiniProperties.getAppId()) || StringUtils.isEmpty(wechatMiniProperties.getAppSecret()))
        {
            throw new ServiceException("微信小程序配置不完整，请检查 appId 和 appSecret");
        }
    }

    private void validateWechatResponse(JSONObject response, String defaultMessage)
    {
        if (response == null)
        {
            throw new ServiceException(defaultMessage);
        }
        Integer errCode = response.getInteger("errcode");
        if (errCode != null && errCode != 0)
        {
            String errMsg = response.getString("errmsg");
            throw new ServiceException(defaultMessage + "：" + StringUtils.defaultIfBlank(errMsg, String.valueOf(errCode)));
        }
    }

    private JSONObject doGet(String requestUrl)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoInput(true);
            return parseResponse(connection);
        }
        catch (IOException ex)
        {
            throw new ServiceException("调用微信接口失败：" + ex.getMessage());
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private JSONObject doPostJson(String requestUrl, String requestBody)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            try (OutputStream outputStream = connection.getOutputStream())
            {
                outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            return parseResponse(connection);
        }
        catch (IOException ex)
        {
            throw new ServiceException("调用微信接口失败：" + ex.getMessage());
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private JSONObject parseResponse(HttpURLConnection connection) throws IOException
    {
        int status = connection.getResponseCode();
        InputStream inputStream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (inputStream == null)
        {
            throw new ServiceException("微信接口无响应内容");
        }
        try (InputStream stream = inputStream)
        {
            String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseObject(body);
        }
    }

    private String encode(String value)
    {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8);
    }

    public static class Code2SessionResult
    {
        private String openId;
        private String sessionKey;
        private String unionId;

        public String getOpenId()
        {
            return openId;
        }

        public void setOpenId(String openId)
        {
            this.openId = openId;
        }

        public String getSessionKey()
        {
            return sessionKey;
        }

        public void setSessionKey(String sessionKey)
        {
            this.sessionKey = sessionKey;
        }

        public String getUnionId()
        {
            return unionId;
        }

        public void setUnionId(String unionId)
        {
            this.unionId = unionId;
        }
    }
}
