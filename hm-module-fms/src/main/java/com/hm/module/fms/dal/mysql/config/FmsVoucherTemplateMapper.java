package com.hm.module.fms.dal.mysql.config;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.fms.dal.dataobject.config.FmsVoucherTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * FMS 凭证模板 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmsVoucherTemplateMapper extends BaseMapperX<FmsVoucherTemplateDO> {

    default List<FmsVoucherTemplateDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<FmsVoucherTemplateDO>()
                .eq(FmsVoucherTemplateDO::getAccountSetId, accountSetId)
                .orderByAsc(FmsVoucherTemplateDO::getCategoryId)
                .orderByAsc(FmsVoucherTemplateDO::getId));
    }

    default Long selectCountByCategoryId(Long categoryId) {
        return selectCount(FmsVoucherTemplateDO::getCategoryId, categoryId);
    }

}
