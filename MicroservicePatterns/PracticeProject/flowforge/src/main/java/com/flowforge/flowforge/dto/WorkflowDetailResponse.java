package com.flowforge.flowforge.dto;

import com.flowforge.flowforge.entity.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowDetailResponse(
        UUID id,
        String name,
        String description,
        WorkflowStatus status,
        int maxRetries,
        int timeoutSeconds,
        Instant createdAt,
        Instant updatedAt,
        List<WorkflowStepResponse> steps
) {
}
