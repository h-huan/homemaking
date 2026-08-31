package com.hm.homemaking.controller.admin;

import com.hm.common.annotation.Log;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.common.core.page.TableDataInfo;
import com.hm.common.enums.BusinessType;
import com.hm.homemaking.domain.HmOrder;
import com.hm.homemaking.dto.AdminOrderAssignRequest;
import com.hm.homemaking.dto.AdminOrderCancelRequest;
import com.hm.homemaking.dto.AdminOrderRefundRequest;
import com.hm.homemaking.service.HmAdminService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/hm/order")
public class HmOrderAdminController extends BaseController
{
    @Autowired
    private HmAdminService adminService;

    @PreAuthorize("@ss.hasPermi('hm:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(HmOrder query)
    {
        startPage();
        List<HmOrder> list = adminService.listOrders(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('hm:order:detail')")
    @GetMapping("/{orderId}")
    public AjaxResult getInfo(@PathVariable Long orderId)
    {
        return success(adminService.getOrder(orderId));
    }

    @PreAuthorize("@ss.hasPermi('hm:order:assign')")
    @Log(title = "Order Assign", businessType = BusinessType.UPDATE)
    @PostMapping("/assign")
    public AjaxResult assign(@RequestBody AdminOrderAssignRequest request)
    {
        return toAjax(adminService.assignOrder(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:order:cancel')")
    @Log(title = "Order Cancel", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestBody AdminOrderCancelRequest request)
    {
        return toAjax(adminService.cancelOrder(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('hm:order:cancel')")
    @Log(title = "Order Refund", businessType = BusinessType.UPDATE)
    @PostMapping("/refund")
    public AjaxResult refund(@RequestBody AdminOrderRefundRequest request)
    {
        return toAjax(adminService.refundOrder(request, getUsername()));
    }
}
