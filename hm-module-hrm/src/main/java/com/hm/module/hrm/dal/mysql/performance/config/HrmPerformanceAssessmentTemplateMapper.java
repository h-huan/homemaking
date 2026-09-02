package com.hm.module.hrm.dal.mysql.performance.config;

import com.hm.framework.common.enums.CommonStatusEnum;
import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.hrm.controller.admin.performance.vo.assessmenttemplate.HrmPerformanceAssessmentTemplatePageReqVO;
import com.hm.module.hrm.dal.dataobject.performance.config.HrmPerformanceAssessmentTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmPerformanceAssessmentTemplateMapper extends BaseMapperX<HrmPerformanceAssessmentTemplateDO> {

    default PageResult<HrmPerformanceAssessmentTemplateDO> selectPage(
            HrmPerformanceAssessmentTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmPerformanceAssessmentTemplateDO>()
                .likeIfPresent(HrmPerformanceAssessmentTemplateDO::getName, reqVO.getName())
                .eq(HrmPerformanceAssessmentTemplateDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByDesc(HrmPerformanceAssessmentTemplateDO::getUpdateTime)
                .orderByDesc(HrmPerformanceAssessmentTemplateDO::getId));
    }

    default HrmPerformanceAssessmentTemplateDO selectByName(String name) {
        return selectFirstOne(HrmPerformanceAssessmentTemplateDO::getName, name,
                HrmPerformanceAssessmentTemplateDO::getStatus, CommonStatusEnum.ENABLE.getStatus());
    }

    default List<HrmPerformanceAssessmentTemplateDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<HrmPerformanceAssessmentTemplateDO>()
                .eq(HrmPerformanceAssessmentTemplateDO::getStatus, status)
                .orderByDesc(HrmPerformanceAssessmentTemplateDO::getUpdateTime)
                .orderByDesc(HrmPerformanceAssessmentTemplateDO::getId));
    }

}
