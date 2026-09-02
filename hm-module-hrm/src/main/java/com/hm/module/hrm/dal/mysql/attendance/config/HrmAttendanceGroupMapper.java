package com.hm.module.hrm.dal.mysql.attendance.config;

import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.mybatis.core.mapper.BaseMapperX;
import com.hm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.hm.module.hrm.controller.admin.attendance.vo.group.HrmAttendanceGroupPageReqVO;
import com.hm.module.hrm.dal.dataobject.attendance.config.HrmAttendanceGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HrmAttendanceGroupMapper extends BaseMapperX<HrmAttendanceGroupDO> {

    default PageResult<HrmAttendanceGroupDO> selectPage(HrmAttendanceGroupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrmAttendanceGroupDO>()
                .likeIfPresent(HrmAttendanceGroupDO::getName, reqVO.getName())
                .eqIfPresent(HrmAttendanceGroupDO::getDefaultStatus, reqVO.getDefaultStatus())
                .orderByDesc(HrmAttendanceGroupDO::getId));
    }

    default HrmAttendanceGroupDO selectByName(String name) {
        return selectFirstOne(HrmAttendanceGroupDO::getName, name);
    }

    default List<HrmAttendanceGroupDO> selectListOrderByDefaultStatusAndId() {
        return selectList(new LambdaQueryWrapperX<HrmAttendanceGroupDO>()
                .orderByDesc(HrmAttendanceGroupDO::getDefaultStatus)
                .orderByDesc(HrmAttendanceGroupDO::getId));
    }

}
