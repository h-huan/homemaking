package com.hm.module.fms.dal.mysql.config;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.module.fms.dal.dataobject.config.FmsFinanceParameterDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * FMS 财务参数 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsFinanceParameterMapper extends BaseMapperX<FmsFinanceParameterDO> {

    default FmsFinanceParameterDO selectByAccountSetId(Long accountSetId) {
        return selectOne(FmsFinanceParameterDO::getAccountSetId, accountSetId);
    }

}
