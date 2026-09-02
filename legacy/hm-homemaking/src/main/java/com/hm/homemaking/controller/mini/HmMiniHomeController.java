package com.hm.homemaking.controller.mini;

import com.hm.common.annotation.Anonymous;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.service.HmMiniService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Anonymous
@RestController
@RequestMapping("/mini/home")
public class HmMiniHomeController extends BaseController
{
    @Autowired
    private HmMiniService miniService;

    @GetMapping("/index")
    public AjaxResult index()
    {
        return success(miniService.getHomeIndex());
    }

    @GetMapping("/content/{contentType}")
    public AjaxResult content(@PathVariable String contentType)
    {
        return success(miniService.getContentByType(contentType));
    }
}
