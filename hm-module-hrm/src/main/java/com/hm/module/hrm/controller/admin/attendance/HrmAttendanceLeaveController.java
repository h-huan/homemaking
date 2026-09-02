package com.hm.module.hrm.controller.admin.attendance;

import com.hm.framework.apilog.core.annotation.ApiAccessLog;
import com.hm.framework.common.pojo.CommonResult;
import com.hm.framework.common.pojo.PageParam;
import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.common.util.collection.MapUtils;
import com.hm.framework.common.util.object.BeanUtils;
import com.hm.framework.excel.core.util.ExcelUtils;
import com.hm.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeavePageReqVO;
import com.hm.module.hrm.controller.admin.attendance.vo.leave.HrmAttendanceLeaveRespVO;
import com.hm.module.hrm.dal.dataobject.attendance.record.HrmAttendanceLeaveDO;
import com.hm.module.hrm.dal.dataobject.employee.info.HrmEmployeeDO;
import com.hm.module.hrm.service.attendance.record.HrmAttendanceLeaveService;
import com.hm.module.hrm.service.employee.info.HrmEmployeeService;
import com.hm.module.system.api.dept.DeptApi;
import com.hm.module.system.api.dept.dto.DeptRespDTO;
import com.hm.framework.apilog.core.enums.OperateTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.framework.common.util.collection.CollectionUtils.convertList;
import static com.hm.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - HRM 请假")
@RestController
@RequestMapping("/hrm/attendance/leave")
@Validated
public class HrmAttendanceLeaveController {

    @Resource
    private HrmAttendanceLeaveService attendanceLeaveService;
    @Resource
    private HrmEmployeeService employeeService;

    @Resource
    private DeptApi deptApi;

    @GetMapping("/get")
    @Operation(summary = "获得请假")
    @Parameter(name = "id", description = "请假记录编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasAnyPermissions('hrm:attendance:leave:query', "
            + "'bpm:process-instance:query', 'bpm:task:query')")
    public CommonResult<HrmAttendanceLeaveRespVO> getAttendanceLeave(@RequestParam("id") Long id) {
        HrmAttendanceLeaveDO list = attendanceLeaveService.getAttendanceLeave(id);
        return success(buildAttendanceLeaveRespVO(list));
    }

    @GetMapping("/page")
    @Operation(summary = "获得请假分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:leave:query')")
    public CommonResult<PageResult<HrmAttendanceLeaveRespVO>> getAttendanceLeavePage(
            @Validated HrmAttendanceLeavePageReqVO pageReqVO) {
        PageResult<HrmAttendanceLeaveDO> pageResult = attendanceLeaveService.getAttendanceLeavePage(pageReqVO);
        return success(new PageResult<>(buildAttendanceLeaveRespVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出请假")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:leave:export')")
    @ApiAccessLog(operateType = OperateTypeEnum.EXPORT)
    public void exportAttendanceLeave(@Validated HrmAttendanceLeavePageReqVO exportReqVO,
                                      HttpServletResponse response) throws IOException {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HrmAttendanceLeaveRespVO> list = buildAttendanceLeaveRespVOList(
                attendanceLeaveService.getAttendanceLeaveList(exportReqVO));
        ExcelUtils.write(response, "请假记录.xls", "数据", HrmAttendanceLeaveRespVO.class, list);
    }

    // ==================== 拼接 VO ====================

    private HrmAttendanceLeaveRespVO buildAttendanceLeaveRespVO(HrmAttendanceLeaveDO attendanceLeave) {
        if (attendanceLeave == null) {
            return null;
        }
        return buildAttendanceLeaveRespVOList(Collections.singletonList(attendanceLeave)).get(0);
    }

    private List<HrmAttendanceLeaveRespVO> buildAttendanceLeaveRespVOList(List<HrmAttendanceLeaveDO> list) {
        Map<Long, HrmEmployeeDO> employeeMap = employeeService.getEmployeeMap(
                convertSet(list, HrmAttendanceLeaveDO::getEmployeeId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(employeeMap.values(), HrmEmployeeDO::getDeptId));
        return convertList(list, attendanceLeave -> {
            HrmAttendanceLeaveRespVO respVO = BeanUtils.toBean(attendanceLeave, HrmAttendanceLeaveRespVO.class);
            HrmEmployeeDO employee = employeeMap.get(attendanceLeave.getEmployeeId());
            if (employee == null) {
                return respVO;
            }
            respVO.setEmployeeName(employee.getName()).setJobNumber(employee.getJobNumber())
                    .setDeptId(employee.getDeptId()).setPostName(employee.getPostName());
            MapUtils.findAndThen(deptMap, employee.getDeptId(), dept -> respVO.setDeptName(dept.getName()));
            return respVO;
        });
    }

}
