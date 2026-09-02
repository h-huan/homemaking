package com.hm.module.product.dal.mysql.property;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.product.controller.admin.property.vo.property.ProductPropertyPageReqVO;
import com.hm.module.product.dal.dataobject.property.ProductPropertyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductPropertyMapper extends BaseMapperX<ProductPropertyDO> {

    default PageResult<ProductPropertyDO> selectPage(ProductPropertyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductPropertyDO>()
                .likeIfPresent(ProductPropertyDO::getName, reqVO.getName())
                .betweenIfPresent(ProductPropertyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductPropertyDO::getId));
    }

    default ProductPropertyDO selectByName(String name) {
        return selectOne(ProductPropertyDO::getName, name);
    }

}
