package com.hm.homemaking.controller.admin;

import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.domain.HmMiniHomeConfig;
import com.hm.homemaking.domain.HmMiniNavConfig;
import com.hm.homemaking.domain.HmPortalContent;
import com.hm.homemaking.service.HmAdminService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/hm/mini-config")
public class HmMiniConfigAdminController extends BaseController
{
    @Autowired
    private HmAdminService adminService;

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:list')")
    @GetMapping("/home/list")
    public AjaxResult listHomeConfigs()
    {
        return success(adminService.listHomeConfigs());
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:edit')")
    @PostMapping("/home/save")
    public AjaxResult saveHomeConfigs(@RequestBody List<HmMiniHomeConfig> configs)
    {
        return toAjax(adminService.saveHomeConfigs(configs));
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:list')")
    @GetMapping("/nav/list")
    public AjaxResult listNavConfigs()
    {
        return success(adminService.listNavConfigs());
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:edit')")
    @PostMapping("/nav/save")
    public AjaxResult saveNavConfigs(@RequestBody List<HmMiniNavConfig> configs)
    {
        return toAjax(adminService.saveNavConfigs(configs));
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:list')")
    @GetMapping("/content/list")
    public AjaxResult listPortalContents(HmPortalContent query)
    {
        return success(adminService.listPortalContents(query));
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:list')")
    @GetMapping("/content/{contentId}")
    public AjaxResult getPortalContent(@PathVariable Long contentId)
    {
        return success(adminService.getPortalContent(contentId));
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:edit')")
    @PostMapping("/content/save")
    public AjaxResult savePortalContent(@RequestBody HmPortalContent content)
    {
        return toAjax(adminService.savePortalContent(content));
    }

    @PreAuthorize("@ss.hasPermi('hm:miniConfig:edit')")
    @DeleteMapping("/content/{contentId}")
    public AjaxResult removePortalContent(@PathVariable Long contentId)
    {
        return toAjax(adminService.removePortalContent(contentId));
    }
}
