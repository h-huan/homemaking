package com.hm.homemaking.controller.mini;

import com.hm.common.annotation.Anonymous;
import com.hm.common.core.controller.BaseController;
import com.hm.common.core.domain.AjaxResult;
import com.hm.homemaking.dto.MiniOrderCalcRequest;
import com.hm.homemaking.dto.MiniOrderSubmitRequest;
import com.hm.homemaking.dto.MiniReviewRequest;
import com.hm.homemaking.service.HmMiniService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Anonymous
@RestController
@RequestMapping("/mini/order")
public class HmMiniOrderController extends BaseController
{
    @Autowired
    private HmMiniService miniService;

    @PostMapping("/calc")
    public AjaxResult calc(@RequestBody MiniOrderCalcRequest request)
    {
        return success(miniService.preCalc(request));
    }

    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody MiniOrderSubmitRequest request)
    {
        return success(miniService.submitOrder(request));
    }

    @PostMapping("/pay/{orderId}")
    public AjaxResult pay(@PathVariable Long orderId)
    {
        return success(miniService.payOrder(orderId));
    }

    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String orderStatus,
                           @RequestParam(required = false) Integer pageNum,
                           @RequestParam(required = false) Integer pageSize)
    {
        return success(miniService.listOrders(orderStatus, pageNum, pageSize));
    }

    @GetMapping("/{orderId}")
    public AjaxResult detail(@PathVariable Long orderId)
    {
        return success(miniService.getOrderDetail(orderId));
    }

    @PostMapping("/cancel/{orderId}")
    public AjaxResult cancel(@PathVariable Long orderId, @RequestBody(required = false) Map<String, String> body)
    {
        String cancelReason = body == null ? null : body.get("cancelReason");
        return toAjax(miniService.cancelOrder(orderId, cancelReason));
    }

    @PostMapping("/review")
    public AjaxResult review(@RequestBody MiniReviewRequest request)
    {
        return toAjax(miniService.submitReview(request));
    }
}
