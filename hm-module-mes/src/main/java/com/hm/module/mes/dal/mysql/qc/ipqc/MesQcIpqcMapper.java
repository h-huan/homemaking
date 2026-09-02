package com.hm.module.mes.dal.mysql.qc.ipqc;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.mes.controller.admin.qc.ipqc.vo.MesQcIpqcPageReqVO;
import com.hm.module.mes.dal.dataobject.qc.ipqc.MesQcIpqcDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 过程检验单（IPQC） Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesQcIpqcMapper extends BaseMapperX<MesQcIpqcDO> {

    default PageResult<MesQcIpqcDO> selectPage(MesQcIpqcPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesQcIpqcDO>()
                .likeIfPresent(MesQcIpqcDO::getCode, reqVO.getCode())
                .eqIfPresent(MesQcIpqcDO::getType, reqVO.getType())
                .eqIfPresent(MesQcIpqcDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesQcIpqcDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesQcIpqcDO::getCheckResult, reqVO.getCheckResult())
                .eqIfPresent(MesQcIpqcDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesQcIpqcDO::getInspectorUserId, reqVO.getInspectorUserId())
                .orderByDesc(MesQcIpqcDO::getId));
    }

    default MesQcIpqcDO selectByCode(String code) {
        return selectOne(MesQcIpqcDO::getCode, code);
    }

}
