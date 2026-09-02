package com.hm.module.pay.controller.app.order;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjUtil;
import com.hm.framework.common.pojo.CommonResult;
import com.hm.framework.common.util.object.BeanUtils;
import com.hm.module.pay.controller.admin.order.vo.PayOrderRespVO;
import com.hm.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import com.hm.module.pay.controller.app.order.vo.AppPayOrderSubmitReqVO;
import com.hm.module.pay.controller.app.order.vo.AppPayOrderSubmitRespVO;
import com.hm.module.pay.dal.dataobject.order.PayOrderDO;
import com.hm.module.pay.dal.dataobject.wallet.PayWalletDO;
import com.hm.module.pay.enums.PayChannelEnum;
import com.hm.module.pay.enums.order.PayOrderStatusEnum;
import com.hm.module.pay.framework.pay.core.client.impl.wallet.WalletPayClient;
import com.hm.module.pay.service.order.PayOrderService;
import com.hm.module.pay.service.wallet.PayWalletService;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.framework.common.util.servlet.ServletUtils.getClientIP;
import static com.hm.framework.web.core.util.WebFrameworkUtils.getLoginUserId;
import static com.hm.framework.web.core.util.WebFrameworkUtils.getLoginUserType;

@Tag(name = "用户 APP - 支付订单")
@RestController
@RequestMapping("/pay/order")
@Validated
@Slf4j
public class AppPayOrderController {

    @Resource
    private PayOrderService payOrderService;
    @Resource
    private PayWalletService payWalletService;

    @GetMapping("/get")
    @Operation(summary = "获得支付订单")
    @Parameters({
            @Parameter(name = "id", description = "编号", example = "1024"),
            @Parameter(name = "no", description = "支付订单号", example = "Pxxx"),
            @Parameter(name = "sync", description = "是否同步", example = "true")
    })
    public CommonResult<PayOrderRespVO> getOrder(@RequestParam(value = "id", required = false) Long id,
                                                 @RequestParam(value = "no", required = false) String no,
                                                 @RequestParam(value = "sync", required = false) Boolean sync) {
        PayOrderDO order = null;
        if (CharSequenceUtil.isNotEmpty(no)) {
            order = payOrderService.getOrder(no);
        }
        if (ObjUtil.isNull(order) && ObjUtil.isNotNull(id)) {
            order = payOrderService.getOrder(id);
        }
        if (order == null) {
            return success(null);
        }
        // 重要：校验订单是否是当前用户，避免越权
        if (order.getUserId() == null || getLoginUserId() == null
                || !Objects.equals(order.getUserId(), getLoginUserId())
                || !Objects.equals(order.getUserType(), getLoginUserType())) {
            return success(null);
        }

        // sync 仅在等待支付
        if (Boolean.TRUE.equals(sync) && PayOrderStatusEnum.isWaiting(order.getStatus())) {
            payOrderService.syncOrderQuietly(order.getId());
            // 重新查询，因为同步后，可能会有变化
            order = payOrderService.getOrder(order.getId());
        }
        return success(BeanUtils.toBean(order, PayOrderRespVO.class));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交支付订单")
    public CommonResult<AppPayOrderSubmitRespVO> submitPayOrder(@jakarta.validation.Valid @RequestBody AppPayOrderSubmitReqVO reqVO) {
        PayOrderDO owned = payOrderService.getOrder(reqVO.getId());
        if (owned == null || owned.getUserId() == null || getLoginUserId() == null
                || !Objects.equals(owned.getUserId(), getLoginUserId())
                || !Objects.equals(owned.getUserType(), getLoginUserType())) {
            throw new org.springframework.security.access.AccessDeniedException("无权访问此支付订单");
        }
        // Homemaking payments must use its server-side identity and channel binding.
        if (owned.getMerchantOrderId() != null && owned.getMerchantOrderId().startsWith("HM-")) {
            throw new org.springframework.security.access.AccessDeniedException("请从家政订单发起支付");
        }
        // 1. 钱包支付事，需要额外传 user_id 和 user_type
        if (Objects.equals(reqVO.getChannelCode(), PayChannelEnum.WALLET.getCode())) {
            if (reqVO.getChannelExtras() == null) {
                reqVO.setChannelExtras(Maps.newHashMapWithExpectedSize(1));
            }
            PayWalletDO wallet = payWalletService.getOrCreateWallet(getLoginUserId(), getLoginUserType());
            reqVO.getChannelExtras().put(WalletPayClient.WALLET_ID_KEY, String.valueOf(wallet.getId()));
        }

        // 2. 提交支付
        PayOrderSubmitRespVO respVO = payOrderService.submitOrder(reqVO, getClientIP());
        return success(BeanUtils.toBean(respVO, AppPayOrderSubmitRespVO.class));
    }

}
