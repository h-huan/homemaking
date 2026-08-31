package com.hm.homemaking.controller.mini;

import com.hm.common.annotation.Anonymous;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.service.HmMiniService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Anonymous
@RestController
@RequestMapping("/mini/service")
public class HmMiniServiceController extends BaseController
{
    @Autowired
    private HmMiniService miniService;

    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Integer pageNum,
                           @RequestParam(required = false) Integer pageSize)
    {
        return success(miniService.getServiceList(categoryId, keyword, pageNum, pageSize));
    }

    @GetMapping("/{serviceItemId}")
    public AjaxResult detail(@PathVariable Long serviceItemId)
    {
        return success(miniService.getServiceDetail(serviceItemId));
    }
}
