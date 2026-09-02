package com.hm.module.mes.dal.mysql.md.item;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.mes.controller.admin.md.item.vo.type.MesMdItemTypeListReqVO;
import com.hm.module.mes.dal.dataobject.md.item.MesMdItemTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 物料产品分类 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesMdItemTypeMapper extends BaseMapperX<MesMdItemTypeDO> {

    default List<MesMdItemTypeDO> selectList(MesMdItemTypeListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<MesMdItemTypeDO>()
                .likeIfPresent(MesMdItemTypeDO::getName, reqVO.getName())
                .eqIfPresent(MesMdItemTypeDO::getStatus, reqVO.getStatus())
                .orderByAsc(MesMdItemTypeDO::getSort));
    }

    default MesMdItemTypeDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(MesMdItemTypeDO::getParentId, parentId, MesMdItemTypeDO::getName, name);
    }

    default MesMdItemTypeDO selectByParentIdAndCode(Long parentId, String code) {
        return selectOne(MesMdItemTypeDO::getParentId, parentId, MesMdItemTypeDO::getCode, code);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(MesMdItemTypeDO::getParentId, parentId);
    }

}
