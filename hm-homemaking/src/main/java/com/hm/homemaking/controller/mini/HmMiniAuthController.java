package com.hm.homemaking.controller.mini;

import com.hm.common.annotation.Anonymous;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.dto.MiniBindMobileRequest;
import com.hm.homemaking.dto.MiniLoginRequest;
import com.hm.homemaking.service.HmMiniService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Anonymous
@RestController
@RequestMapping("/mini/auth")
public class HmMiniAuthController extends BaseController
{
    @Autowired
    private HmMiniService miniService;

    @PostMapping("/login")
    public AjaxResult login(@RequestBody MiniLoginRequest request)
    {
        return success(miniService.login(request));
    }

    @PostMapping("/bindMobile")
    public AjaxResult bindMobile(@RequestBody MiniBindMobileRequest request)
    {
        return success(miniService.bindMobile(request));
    }

    @GetMapping("/profile")
    public AjaxResult profile()
    {
        return success(miniService.getCurrentProfile());
    }
}
