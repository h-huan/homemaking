package com.hm.module.mes.controller.admin.pro.process;

import com.hm.framework.apilog.core.annotation.ApiAccessLog;
import com.hm.framework.common.enums.CommonStatusEnum;
import com.hm.framework.common.pojo.CommonResult;
import com.hm.framework.common.pojo.PageParam;
import com.hm.framework.common.pojo.PageResult;
import com.hm.framework.common.util.object.BeanUtils;
import com.hm.framework.excel.core.util.ExcelUtils;
import com.hm.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO;
import com.hm.module.mes.controller.admin.pro.process.vo.MesProProcessRespVO;
import com.hm.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO;
import com.hm.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import com.hm.module.mes.service.pro.process.MesProProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static com.hm.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static com.hm.framework.common.pojo.CommonResult.success;
import static com.hm.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - MES 生产工序")
@RestController
@RequestMapping("/mes/pro/process")
@Validated
public class MesProProcessController {

    @Resource
    private MesProProcessService processService;

    @PostMapping("/create")
    @Operation(summary = "创建生产工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-process:create')")
    public CommonResult<Long> createProcess(@Valid @RequestBody MesProProcessSaveReqVO createReqVO) {
        return success(processService.createProcess(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新生产工序")
    @PreAuthorize("@ss.hasPermission('mes:pro-process:update')")
    public CommonResult<Boolean> updateProcess(@Valid @RequestBody MesProProcessSaveReqVO updateReqVO) {
        processService.updateProcess(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除生产工序")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('mes:pro-process:delete')")
    public CommonResult<Boolean> deleteProcess(@RequestParam("id") Long id) {
        processService.deleteProcess(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得生产工序")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:pro-process:query')")
    public CommonResult<MesProProcessRespVO> getProcess(@RequestParam("id") Long id) {
        MesProProcessDO process = processService.getProcess(id);
        return success(BeanUtils.toBean(process, MesProProcessRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得生产工序分页")
    @PreAuthorize("@ss.hasPermission('mes:pro-process:query')")
    public CommonResult<PageResult<MesProProcessRespVO>> getProcessPage(@Valid MesProProcessPageReqVO pageReqVO) {
        PageResult<MesProProcessDO> pageResult = processService.getProcessPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesProProcessRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得生产工序精简列表", description = "只包含被开启的工序，主要用于前端的下拉选项")
    public CommonResult<List<MesProProcessRespVO>> getProcessSimpleList() {
        List<MesProProcessDO> list = processService.getProcessListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, process -> new MesProProcessRespVO()
                .setId(process.getId()).setName(process.getName()).setCode(process.getCode())
                .setAttention(process.getAttention()).setRemark(process.getRemark())));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出生产工序 Excel")
    @PreAuthorize("@ss.hasPermission('mes:pro-process:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProcessExcel(@Valid MesProProcessPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesProProcessDO> list = processService.getProcessPage(pageReqVO).getList();
        List<MesProProcessRespVO> data = BeanUtils.toBean(list, MesProProcessRespVO.class);
        ExcelUtils.write(response, "生产工序.xls", "数据", MesProProcessRespVO.class, data);
    }

}
