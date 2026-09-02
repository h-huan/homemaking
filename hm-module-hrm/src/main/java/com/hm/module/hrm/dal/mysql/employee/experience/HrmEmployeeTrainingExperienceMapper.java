package com.hm.module.hrm.dal.mysql.employee.experience;

import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.hrm.dal.dataobject.employee.experience.HrmEmployeeTrainingExperienceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmEmployeeTrainingExperienceMapper extends BaseMapperX<HrmEmployeeTrainingExperienceDO> {

    default List<HrmEmployeeTrainingExperienceDO> selectListByEmployeeId(Long employeeId) {
        return selectList(new LambdaQueryWrapperX<HrmEmployeeTrainingExperienceDO>()
                .eq(HrmEmployeeTrainingExperienceDO::getEmployeeId, employeeId)
                .orderByAsc(HrmEmployeeTrainingExperienceDO::getSort)
                .orderByDesc(HrmEmployeeTrainingExperienceDO::getId));
    }

}
