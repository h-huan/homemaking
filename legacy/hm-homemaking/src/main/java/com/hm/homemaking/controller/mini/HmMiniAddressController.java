package com.hm.homemaking.controller.mini;

import com.hm.common.annotation.Anonymous;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.domain.HmCustomerAddress;
import com.hm.homemaking.service.HmMiniService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Anonymous
@RestController
@RequestMapping("/mini/address")
public class HmMiniAddressController extends BaseController
{
    @Autowired
    private HmMiniService miniService;

    @GetMapping("/list")
    public AjaxResult list()
    {
        return success(miniService.listAddresses());
    }

    @GetMapping("/{addressId}")
    public AjaxResult detail(@PathVariable Long addressId)
    {
        return success(miniService.getAddress(addressId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody HmCustomerAddress address)
    {
        return toAjax(miniService.saveAddress(address));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody HmCustomerAddress address)
    {
        return toAjax(miniService.saveAddress(address));
    }

    @DeleteMapping("/{addressId}")
    public AjaxResult remove(@PathVariable Long addressId)
    {
        return toAjax(miniService.deleteAddress(addressId));
    }
}
