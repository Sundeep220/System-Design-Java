package com.flowforge.flowforge.controller;

import com.flowforge.flowforge.config.FlowForgeProperties;
import com.flowforge.flowforge.service.WorkflowService;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
public class DebugController {

    private final WorkflowService workflowService;
    private final FlowForgeProperties properties;
    private final Environment environment;

    public DebugController(WorkflowService workflowService,
                           FlowForgeProperties properties,
                           Environment environment) {
        this.workflowService = workflowService;
        this.properties = properties;
        this.environment = environment;
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

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> configInfo() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("activeProfiles", environment.getActiveProfiles());
        config.put("flowforge.execution.defaultMaxRetries", properties.execution().defaultMaxRetries());
        config.put("flowforge.execution.defaultTimeoutSeconds", properties.execution().defaultTimeoutSeconds());
        config.put("flowforge.rateLimit.maxRequests", properties.rateLimit().maxRequests());
        config.put("flowforge.rateLimit.windowSeconds", properties.rateLimit().windowSeconds());
        config.put("spring.threads.virtual.enabled", environment.getProperty("spring.threads.virtual.enabled", "false"));
        config.put("server.port", environment.getProperty("server.port", "8080"));
        config.put("currentThread", Thread.currentThread().toString());
        return ResponseEntity.ok(config);
    }

    @GetMapping("/retry")
    public ResponseEntity<Map<String, String>> testRetry() {
        String result = workflowService.simulateTransientFailure();
        return ResponseEntity.ok(Map.of("result", result));
    }
}
