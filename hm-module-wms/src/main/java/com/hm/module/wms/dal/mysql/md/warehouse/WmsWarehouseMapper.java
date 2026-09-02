package com.hm.module.wms.dal.mysql.md.warehouse;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.wms.controller.admin.md.warehouse.vo.WmsWarehousePageReqVO;
import com.hm.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * WMS 仓库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsWarehouseMapper extends BaseMapperX<WmsWarehouseDO> {

    default PageResult<WmsWarehouseDO> selectPage(WmsWarehousePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsWarehouseDO>()
                .eqIfPresent(WmsWarehouseDO::getCode, reqVO.getCode())
                .likeIfPresent(WmsWarehouseDO::getName, reqVO.getName())
                .orderByAsc(WmsWarehouseDO::getSort)
                .orderByDesc(WmsWarehouseDO::getId));
    }

    default WmsWarehouseDO selectByCode(String code) {
        return selectOne(WmsWarehouseDO::getCode, code);
    }

    default WmsWarehouseDO selectByName(String name) {
        return selectOne(WmsWarehouseDO::getName, name);
    }

    default List<WmsWarehouseDO> selectSimpleList() {
        return selectList(new LambdaQueryWrapperX<WmsWarehouseDO>()
                .orderByAsc(WmsWarehouseDO::getSort)
                .orderByDesc(WmsWarehouseDO::getId));
    }

}
