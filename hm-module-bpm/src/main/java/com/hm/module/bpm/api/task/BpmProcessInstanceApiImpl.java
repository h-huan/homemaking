package com.hm.module.bpm.api.task;

import com.hm.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import com.hm.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import com.hm.module.bpm.service.task.BpmProcessInstanceService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Flowable 流程实例 Api 实现类
 *
 * @author 芋道源码
 * @author jason
 */
@Service
@Validated
public class BpmProcessInstanceApiImpl implements BpmProcessInstanceApi {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Override
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO) {
        return processInstanceService.createProcessInstance(userId, reqDTO);
    }

    @Override
    public void cancelProcessInstanceByStartUser(Long userId, String processInstanceId, String reason) {
        processInstanceService.cancelProcessInstanceByStartUser(userId,
                new BpmProcessInstanceCancelReqVO().setId(processInstanceId).setReason(reason));
    }

}
