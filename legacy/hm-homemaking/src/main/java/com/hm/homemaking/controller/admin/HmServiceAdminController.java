package com.hm.homemaking.controller.admin;

import com.hm.common.annotation.Log;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.common.core.page.TableDataInfo;
import com.hm.common.enums.BusinessType;
import com.hm.homemaking.domain.HmServiceItem;
import com.hm.homemaking.service.HmAdminService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/hm/service")
public class HmServiceAdminController extends BaseController
{
    @Autowired
    private HmAdminService adminService;

    @PreAuthorize("@ss.hasPermi('hm:service:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmServiceItem query)
    {
        startPage();
        List<HmServiceItem> list = adminService.listServices(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('hm:service:list')")
    @GetMapping("/{serviceItemId}")
    public AjaxResult getInfo(@PathVariable Long serviceItemId)
    {
        return success(adminService.getService(serviceItemId));
    }

    @PreAuthorize("@ss.hasPermi('hm:service:add')")
    @Log(title = "Service Item", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmServiceItem serviceItem)
    {
        return toAjax(adminService.saveService(serviceItem, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:service:edit')")
    @Log(title = "Service Item", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmServiceItem serviceItem)
    {
        return toAjax(adminService.saveService(serviceItem, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:service:edit')")
    @Log(title = "Service Item", businessType = BusinessType.DELETE)
    @DeleteMapping("/{serviceItemId}")
    public AjaxResult remove(@PathVariable Long serviceItemId)
    {
        return toAjax(adminService.removeService(serviceItemId));
    }
}
