package com.hm.module.hrm.controller.admin.operatelog.vo;

import com.hm.framework.common.pojo.PageParam;
import com.hm.framework.common.validation.InEnum;
import com.hm.module.hrm.enums.common.HrmBizTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HRM 操作日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmOperateLogPageReqVO extends PageParam {

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(HrmBizTypeEnum.class)
    @NotNull(message = "业务类型不能为空")
    private Integer bizType;

    @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "业务编号不能为空")
    private Long bizId;

}
