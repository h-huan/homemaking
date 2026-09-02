package com.hm.module.fms.dal.mysql.report.income;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.fms.dal.dataobject.report.income.FmsIncomeStatementConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 利润表配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsIncomeStatementConfigMapper extends BaseMapperX<FmsIncomeStatementConfigDO> {

    default List<FmsIncomeStatementConfigDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsIncomeStatementConfigDO>()
                .eq(FmsIncomeStatementConfigDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsIncomeStatementConfigDO::getSort)
                .orderByAsc(FmsIncomeStatementConfigDO::getId));
    }

}
