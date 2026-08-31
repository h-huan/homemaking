package com.hm.homemaking.controller.mini;

import com.hm.common.annotation.Anonymous;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.service.HmMiniService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Anonymous
@RestController
@RequestMapping("/mini/config")
public class HmMiniConfigController extends BaseController
{
    @Autowired
    private HmMiniService miniService;

    @GetMapping("/nav")
    public AjaxResult nav()
    {
        return success(miniService.getNavConfig());
    }
}
