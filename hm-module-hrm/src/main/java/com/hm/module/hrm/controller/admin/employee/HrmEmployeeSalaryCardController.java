package com.hm.module.hrm.controller.admin.employee;

import com.hm.framework.common.pojo.CommonResult;
import com.hm.framework.common.util.object.BeanUtils;
import com.hm.framework.ip.core.utils.AreaUtils;
import com.hm.module.hrm.controller.admin.employee.vo.salarycard.HrmEmployeeSalaryCardRespVO;
import com.hm.module.hrm.controller.admin.employee.vo.salarycard.HrmEmployeeSalaryCardSaveReqVO;
import com.hm.module.hrm.dal.dataobject.employee.employment.HrmEmployeeSalaryCardDO;
import com.hm.module.hrm.service.employee.employment.HrmEmployeeSalaryCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.hm.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HRM 员工工资卡")
@RestController
@RequestMapping("/hrm/employee/salary-card")
@Validated
public class HrmEmployeeSalaryCardController {

    @Resource
    private HrmEmployeeSalaryCardService salaryCardService;

    @GetMapping("/get")
    @Operation(summary = "获得员工工资卡")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmEmployeeSalaryCardRespVO> getEmployeeSalaryCard(
            @RequestParam("employeeId") Long employeeId) {
        HrmEmployeeSalaryCardDO salaryCard = salaryCardService.getSalaryCardByEmployeeId(employeeId);
        if (salaryCard == null) {
            return success(null);
        }
        HrmEmployeeSalaryCardRespVO respVO = BeanUtils.toBean(salaryCard, HrmEmployeeSalaryCardRespVO.class)
                .setBankAreaName(AreaUtils.format(salaryCard.getBankAreaId()));
        return success(respVO);
    }

    @PutMapping("/save")
    @Operation(summary = "保存员工工资卡")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> saveEmployeeSalaryCard(
            @Valid @RequestBody HrmEmployeeSalaryCardSaveReqVO reqVO) {
        return success(salaryCardService.saveSalaryCard(reqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工工资卡")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> deleteEmployeeSalaryCard(@RequestParam("employeeId") Long employeeId) {
        salaryCardService.deleteSalaryCardByEmployeeId(employeeId);
        return success(true);
    }

}
