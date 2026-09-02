package com.hm.module.mes.dal.mysql.wm.miscreceipt;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.mes.controller.admin.wm.miscreceipt.vo.MesWmMiscReceiptPageReqVO;
import com.hm.module.mes.dal.dataobject.wm.miscreceipt.MesWmMiscReceiptDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 杂项入库单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesWmMiscReceiptMapper extends BaseMapperX<MesWmMiscReceiptDO> {

    default PageResult<MesWmMiscReceiptDO> selectPage(MesWmMiscReceiptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmMiscReceiptDO>()
                .likeIfPresent(MesWmMiscReceiptDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmMiscReceiptDO::getName, reqVO.getName())
                .eqIfPresent(MesWmMiscReceiptDO::getType, reqVO.getType())
                .likeIfPresent(MesWmMiscReceiptDO::getSourceDocCode, reqVO.getSourceDocCode())
                .eqIfPresent(MesWmMiscReceiptDO::getSourceDocType, reqVO.getSourceDocType())
                .eqIfPresent(MesWmMiscReceiptDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesWmMiscReceiptDO::getReceiptDate, reqVO.getReceiptDate())
                .orderByDesc(MesWmMiscReceiptDO::getId));
    }

    default MesWmMiscReceiptDO selectByCode(String code) {
        return selectOne(MesWmMiscReceiptDO::getCode, code);
    }

}
