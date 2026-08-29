package com.flowforge.flowforge.controller;

import com.flowforge.flowforge.service.WorkflowService;
import org.springframework.aop.support.AopUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {

    private final WorkflowService workflowService;

    public DebugController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/aop")
    public ResponseEntity<Map<String, Object>> aopInfo() {
        return ResponseEntity.ok(Map.of(
                "isAopProxy", AopUtils.isAopProxy(workflowService),
                "isCglibProxy", AopUtils.isCglibProxy(workflowService),
                "targetClass", AopUtils.getTargetClass(workflowService).getSimpleName(),
                "actualClass", workflowService.getClass().getSimpleName()
        ));
    }
}
