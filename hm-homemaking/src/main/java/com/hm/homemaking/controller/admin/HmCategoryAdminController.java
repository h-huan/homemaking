package com.hm.homemaking.controller.admin;

import com.hm.common.annotation.Log;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.common.core.page.TableDataInfo;
import com.hm.common.enums.BusinessType;
import com.hm.homemaking.domain.HmServiceCategory;
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
@RequestMapping("/admin/hm/category")
public class HmCategoryAdminController extends BaseController
{
    @Autowired
    private HmAdminService adminService;

    @PreAuthorize("@ss.hasPermi('hm:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmServiceCategory query)
    {
        startPage();
        List<HmServiceCategory> list = adminService.listCategories(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('hm:category:list')")
    @GetMapping("/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId)
    {
        return success(adminService.getCategory(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('hm:category:add')")
    @Log(title = "Service Category", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmServiceCategory category)
    {
        return toAjax(adminService.saveCategory(category, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:category:edit')")
    @Log(title = "Service Category", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmServiceCategory category)
    {
        return toAjax(adminService.saveCategory(category, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:category:edit')")
    @Log(title = "Service Category", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryId}")
    public AjaxResult remove(@PathVariable Long categoryId)
    {
        return toAjax(adminService.removeCategory(categoryId));
    }
}
