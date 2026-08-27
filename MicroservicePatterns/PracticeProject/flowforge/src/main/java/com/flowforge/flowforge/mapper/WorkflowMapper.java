package com.flowforge.flowforge.mapper;

import com.flowforge.flowforge.dto.PageResponse;
import com.flowforge.flowforge.dto.WorkflowCreateRequest;
import com.flowforge.flowforge.dto.WorkflowDetailResponse;
import com.flowforge.flowforge.dto.WorkflowStepResponse;
import com.flowforge.flowforge.dto.WorkflowSummaryResponse;
import com.flowforge.flowforge.dto.WorkflowPatchRequest;
import com.flowforge.flowforge.dto.WorkflowUpdateRequest;
import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStep;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowMapper {

    public Workflow toEntity(WorkflowCreateRequest request) {
        return new Workflow(
                request.name(),
                request.description(),
                request.maxRetries(),
                request.timeoutSeconds()
        );
    }

    public void updateEntity(Workflow workflow, WorkflowUpdateRequest request) {
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setMaxRetries(request.maxRetries());
        workflow.setTimeoutSeconds(request.timeoutSeconds());
    }

    public void patchEntity(Workflow workflow, WorkflowPatchRequest request) {
        request.name().ifPresent(workflow::setName);
        request.description().ifPresent(workflow::setDescription);
        request.maxRetries().ifPresent(workflow::setMaxRetries);
        request.timeoutSeconds().ifPresent(workflow::setTimeoutSeconds);
    }

    public WorkflowSummaryResponse toSummary(Workflow workflow) {
        return new WorkflowSummaryResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getStatus(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }

    public WorkflowDetailResponse toDetail(Workflow workflow) {
        List<WorkflowStepResponse> stepResponses = workflow.getSteps().stream()
                .map(this::toStepResponse)
                .toList();

        return new WorkflowDetailResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                workflow.getMaxRetries(),
                workflow.getTimeoutSeconds(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt(),
                stepResponses
        );
    }

    public WorkflowStepResponse toStepResponse(WorkflowStep step) {
        return new WorkflowStepResponse(
                step.getId(),
                step.getName(),
                step.getType(),
                step.getStepOrder(),
                step.getConfig(),
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }

    public <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
