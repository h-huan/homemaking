package com.hm.module.mes.dal.mysql.wm.itemconsume;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.module.mes.dal.dataobject.wm.itemconsume.MesWmItemConsumeDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 物料消耗记录明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesWmItemConsumeDetailMapper extends BaseMapperX<MesWmItemConsumeDetailDO> {

    default List<MesWmItemConsumeDetailDO> selectListByConsumeId(Long consumeId) {
        return selectList(MesWmItemConsumeDetailDO::getConsumeId, consumeId);
    }

    default List<MesWmItemConsumeDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmItemConsumeDetailDO::getLineId, lineId);
    }

}
