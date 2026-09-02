package com.hm.module.pay.convert.wallet;

import com.hm.framework.common.pojo.PageResult;
import com.hm.module.pay.controller.admin.wallet.vo.transaction.PayWalletTransactionRespVO;
import com.hm.module.pay.controller.app.wallet.vo.transaction.AppPayWalletTransactionRespVO;
import com.hm.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import com.hm.module.pay.service.wallet.bo.WalletTransactionCreateReqBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayWalletTransactionConvert {

    PayWalletTransactionConvert INSTANCE = Mappers.getMapper(PayWalletTransactionConvert.class);

    PageResult<PayWalletTransactionRespVO> convertPage2(PageResult<PayWalletTransactionDO> page);

    PayWalletTransactionDO convert(WalletTransactionCreateReqBO bean);

}
