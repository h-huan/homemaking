package com.hm.module.mes.dal.mysql.wm.transaction;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.module.mes.dal.dataobject.wm.transaction.MesWmTransactionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 库存事务流水 Mapper
 */
@Mapper
public interface MesWmTransactionMapper extends BaseMapperX<MesWmTransactionDO> {

}
