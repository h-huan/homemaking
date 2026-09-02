package com.hm.module.mes.dal.mysql.wm.transfer;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.mes.controller.admin.wm.transfer.vo.MesWmTransferPageReqVO;
import com.hm.module.mes.dal.dataobject.wm.transfer.MesWmTransferDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 转移单 Mapper
 */
@Mapper
public interface MesWmTransferMapper extends BaseMapperX<MesWmTransferDO> {

    default PageResult<MesWmTransferDO> selectPage(MesWmTransferPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmTransferDO>()
                .likeIfPresent(MesWmTransferDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmTransferDO::getName, reqVO.getName())
                .eqIfPresent(MesWmTransferDO::getType, reqVO.getType())
                .eqIfPresent(MesWmTransferDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesWmTransferDO::getId));
    }

    default MesWmTransferDO selectByCode(String code) {
        return selectOne(MesWmTransferDO::getCode, code);
    }

}
