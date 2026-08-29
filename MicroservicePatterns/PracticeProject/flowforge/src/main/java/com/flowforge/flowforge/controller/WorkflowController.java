package com.flowforge.flowforge.controller;

import com.flowforge.flowforge.dto.CursorPageResponse;
import com.flowforge.flowforge.dto.PageResponse;
import com.flowforge.flowforge.dto.WorkflowCreateRequest;
import com.flowforge.flowforge.dto.WorkflowDetailResponse;
import com.flowforge.flowforge.dto.WorkflowFilterRequest;
import com.flowforge.flowforge.dto.WorkflowPatchRequest;
import com.flowforge.flowforge.dto.WorkflowSummaryResponse;
import com.flowforge.flowforge.dto.WorkflowUpdateRequest;
import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStatus;
import com.flowforge.flowforge.mapper.WorkflowMapper;
import com.flowforge.flowforge.service.WorkflowService;
import com.flowforge.flowforge.validation.group.OnCreate;
import com.flowforge.flowforge.validation.group.OnUpdate;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowMapper workflowMapper;

    public WorkflowController(WorkflowService workflowService, WorkflowMapper workflowMapper) {
        this.workflowService = workflowService;
        this.workflowMapper = workflowMapper;
    }

    @PostMapping
    public ResponseEntity<WorkflowDetailResponse> create(@Validated(OnCreate.class) @RequestBody WorkflowCreateRequest request) {
        Workflow workflow = workflowService.create(request);
        WorkflowDetailResponse response = workflowMapper.toDetail(workflow);
        URI location = URI.create("/api/v1/workflows/" + workflow.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkflowSummaryResponse>> list(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) WorkflowStatus status,
            @RequestParam(required = false) Integer minRetries,
            @RequestParam(required = false) Integer maxRetries,
            @RequestParam(required = false) Instant createdAfter,
            @RequestParam(required = false) Instant createdBefore) {

        WorkflowFilterRequest filter = new WorkflowFilterRequest(
                search, status, minRetries, maxRetries, createdAfter, createdBefore);

        Page<WorkflowSummaryResponse> page = workflowService.findAll(filter, pageable)
                .map(workflowMapper::toSummary);
        return ResponseEntity.ok(workflowMapper.toPageResponse(page));
    }

    @GetMapping(params = "mode=cursor")
    public ResponseEntity<CursorPageResponse<WorkflowSummaryResponse>> listWithCursor(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(workflowService.findAllWithCursor(cursor, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowDetailResponse> getById(@PathVariable UUID id) {
        Workflow workflow = workflowService.findByIdWithSteps(id);
        return ResponseEntity.ok(workflowMapper.toDetail(workflow));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowDetailResponse> update(
            @PathVariable UUID id,
            @Validated(OnUpdate.class) @RequestBody WorkflowUpdateRequest request) {
        Workflow workflow = workflowService.update(id, request);
        return ResponseEntity.ok(workflowMapper.toDetail(workflow));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkflowDetailResponse> patch(
            @PathVariable UUID id,
            @Valid @RequestBody WorkflowPatchRequest request) {
        Workflow workflow = workflowService.patch(id, request);
        return ResponseEntity.ok(workflowMapper.toDetail(workflow));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        workflowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
