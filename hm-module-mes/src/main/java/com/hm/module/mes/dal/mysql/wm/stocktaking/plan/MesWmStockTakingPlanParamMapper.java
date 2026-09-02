package com.hm.module.mes.dal.mysql.wm.stocktaking.plan;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.mes.controller.admin.wm.stocktaking.plan.vo.param.MesWmStockTakingPlanParamPageReqVO;
import com.hm.module.mes.dal.dataobject.wm.stocktaking.plan.MesWmStockTakingPlanParamDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 盘点方案参数 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesWmStockTakingPlanParamMapper extends BaseMapperX<MesWmStockTakingPlanParamDO> {

    default PageResult<MesWmStockTakingPlanParamDO> selectPage(MesWmStockTakingPlanParamPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmStockTakingPlanParamDO>()
                .eqIfPresent(MesWmStockTakingPlanParamDO::getPlanId, reqVO.getPlanId())
                .orderByAsc(MesWmStockTakingPlanParamDO::getId));
    }

    default List<MesWmStockTakingPlanParamDO> selectListByPlanId(Long planId) {
        return selectList(MesWmStockTakingPlanParamDO::getPlanId, planId);
    }

    default void deleteByPlanId(Long planId) {
        delete(MesWmStockTakingPlanParamDO::getPlanId, planId);
    }

}
