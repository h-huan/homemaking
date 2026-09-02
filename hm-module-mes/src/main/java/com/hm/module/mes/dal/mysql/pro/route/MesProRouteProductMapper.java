package com.hm.module.mes.dal.mysql.pro.route;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 工艺路线产品 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProRouteProductMapper extends BaseMapperX<MesProRouteProductDO> {

    default List<MesProRouteProductDO> selectListByRouteId(Long routeId) {
        return selectList(MesProRouteProductDO::getRouteId, routeId);
    }

    default MesProRouteProductDO selectByItemId(Long itemId) {
        return selectOne(MesProRouteProductDO::getItemId, itemId);
    }

    default void deleteByRouteId(Long routeId) {
        delete(new LambdaQueryWrapperX<MesProRouteProductDO>()
                .eq(MesProRouteProductDO::getRouteId, routeId));
    }

}
