package com.hm.homemaking.service.impl;

import com.hm.common.constant.Constants;
import com.hm.common.constant.HttpStatus;
import com.hm.common.core.redis.RedisCache;
import com.hm.common.exception.ServiceException;
import com.hm.common.utils.ServletUtils;
import com.hm.common.utils.StringUtils;
import com.hm.common.utils.ip.IpUtils;
import com.hm.common.utils.uuid.IdUtils;
import com.hm.homemaking.domain.HmCustomerUser;
import com.hm.homemaking.domain.HmMiniLoginUser;
import com.hm.homemaking.mapper.HmCustomerMapper;
import com.hm.homemaking.service.HmAppAuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HmAppAuthServiceImpl implements HmAppAuthService
{
    private static final String MINI_TOKEN_PREFIX = "mini_login_tokens:";

    @Value("${token.header}")
    private String header;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private HmCustomerMapper customerMapper;

    @Override
    public String createToken(HmCustomerUser customerUser, String openId)
    {
        String token = IdUtils.fastUUID();
        refreshLogin(customerUser, openId, token);
        return token;
    }

    @Override
    public HmMiniLoginUser getLoginUser()
    {
        HttpServletRequest request = ServletUtils.getRequest();
        String token = request.getHeader(header);
        if (StringUtils.isEmpty(token))
        {
            throw new ServiceException("not logged in", HttpStatus.UNAUTHORIZED);
        }
        if (token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.substring(Constants.TOKEN_PREFIX.length());
        }
        HmMiniLoginUser loginUser = redisCache.getCacheObject(MINI_TOKEN_PREFIX + token);
        if (loginUser == null)
        {
            throw new ServiceException("login expired", HttpStatus.UNAUTHORIZED);
        }
        return loginUser;
    }

    @Override
    public HmCustomerUser getCurrentCustomer()
    {
        HmMiniLoginUser loginUser = getLoginUser();
        HmCustomerUser customerUser = customerMapper.selectCustomerById(loginUser.getCustomerId());
        if (customerUser == null)
        {
            throw new ServiceException("customer is unavailable", HttpStatus.UNAUTHORIZED);
        }
        return customerUser;
    }

    @Override
    public void refreshLogin(HmCustomerUser customerUser, String openId, String token)
    {
        HmMiniLoginUser loginUser = new HmMiniLoginUser();
        loginUser.setCustomerId(customerUser.getCustomerId());
        loginUser.setMobile(customerUser.getMobile());
        loginUser.setNickname(customerUser.getNickname());
        loginUser.setAvatar(customerUser.getAvatar());
        loginUser.setOpenId(openId);
        loginUser.setToken(token);
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + TimeUnit.DAYS.toMillis(7));
        redisCache.setCacheObject(MINI_TOKEN_PREFIX + token, loginUser, 7, TimeUnit.DAYS);

        customerUser.setLastLoginIp(IpUtils.getIpAddr());
        customerMapper.updateCustomer(customerUser);
    }
}
