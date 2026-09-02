package com.hm.homemaking.controller.admin;

import com.hm.common.annotation.Log;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.common.core.page.TableDataInfo;
import com.hm.common.enums.BusinessType;
import com.hm.homemaking.domain.HmWorkerProfile;
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
@RequestMapping("/admin/hm/worker")
public class HmWorkerAdminController extends BaseController
{
    @Autowired
    private HmAdminService adminService;

    @PreAuthorize("@ss.hasPermi('hm:worker:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmWorkerProfile query)
    {
        startPage();
        List<HmWorkerProfile> list = adminService.listWorkers(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('hm:worker:list')")
    @GetMapping("/{workerId}")
    public AjaxResult getInfo(@PathVariable Long workerId)
    {
        return success(adminService.getWorker(workerId));
    }

    @PreAuthorize("@ss.hasPermi('hm:worker:add')")
    @Log(title = "Worker Profile", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HmWorkerProfile workerProfile)
    {
        return toAjax(adminService.saveWorker(workerProfile, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:worker:edit')")
    @Log(title = "Worker Profile", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody HmWorkerProfile workerProfile)
    {
        return toAjax(adminService.saveWorker(workerProfile, getUsername()));
    }
}
