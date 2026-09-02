package com.hm.module.fms.dal.mysql.config;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.module.fms.dal.dataobject.config.FmsAccountSetDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * FMS 账套 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsAccountSetMapper extends BaseMapperX<FmsAccountSetDO> {

    default FmsAccountSetDO selectByCompanyCode(String companyCode) {
        return selectOne(FmsAccountSetDO::getCompanyCode, companyCode);
    }

    default FmsAccountSetDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(FmsAccountSetDO::getId, id);
    }

}
