package com.hm.module.homemaking.controller;

import com.hm.framework.common.pojo.CommonResult;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Order(-1)
@RestControllerAdvice(basePackages="com.hm.module.homemaking.controller")
public class HomemakingExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CommonResult<?>> status(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(CommonResult.error(error.getStatusCode().value(),error.getReason()==null?"请求不可用":error.getReason()));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonResult<?>> conflict(){return ResponseEntity.status(409).body(CommonResult.error(409,"记录已存在或时段已被预约，请刷新后重试"));}
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResult<?>> invalid(){return ResponseEntity.badRequest().body(CommonResult.error(400,"请求或配置格式无效"));}
}
