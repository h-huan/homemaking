package com.hm.module.mp.dal.mysql.message;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.module.mp.controller.admin.message.vo.template.MpMessageTemplateListReqVO;
import com.hm.module.mp.dal.dataobject.message.MpMessageTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MpMessageTemplateMapper extends BaseMapperX<MpMessageTemplateDO> {

    default List<MpMessageTemplateDO> selectList(MpMessageTemplateListReqVO listReqVO) {
        return selectList(MpMessageTemplateDO::getAccountId, listReqVO.getAccountId());
    }

}