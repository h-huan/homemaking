package com.hm.homemaking.controller.admin;

import com.hm.common.annotation.Log;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.common.core.page.TableDataInfo;
import com.hm.common.enums.BusinessType;
import com.hm.homemaking.domain.HmServiceArea;
import com.hm.homemaking.service.HmAdminService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/hm/area")
public class HmAreaAdminController extends BaseController
{
    @Autowired
    private HmAdminService adminService;

    @PreAuthorize("@ss.hasPermi('hm:area:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmServiceArea query)
    {
        startPage();
        List<HmServiceArea> list = adminService.listAreas(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('hm:area:list')")
    @GetMapping("/{areaId}")
    public AjaxResult getInfo(@PathVariable Long areaId)
    {
        return success(adminService.getArea(areaId));
    }

    @PreAuthorize("@ss.hasPermi('hm:area:add')")
    @Log(title = "Service Area", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmServiceArea area)
    {
        return toAjax(adminService.saveArea(area));
    }

    @PreAuthorize("@ss.hasPermi('hm:area:edit')")
    @Log(title = "Service Area", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmServiceArea area)
    {
        return toAjax(adminService.saveArea(area));
    }
}
