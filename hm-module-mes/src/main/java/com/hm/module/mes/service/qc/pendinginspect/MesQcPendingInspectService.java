package com.hm.module.mes.service.qc.pendinginspect;

import com.hm.framework.common.pojo.PageResult;
import com.hm.module.mes.controller.admin.qc.pendinginspect.vo.MesQcPendingInspectPageReqVO;
import com.hm.module.mes.controller.admin.qc.pendinginspect.vo.MesQcPendingInspectRespVO;

/**
 * MES 待检任务 Service 接口
 */
public interface MesQcPendingInspectService {

    /**
     * 获得待检任务分页
     *
     * @param pageReqVO 分页查询
     * @return 待检任务分页
     */
    PageResult<MesQcPendingInspectRespVO> getPendingInspectPage(MesQcPendingInspectPageReqVO pageReqVO);

}
