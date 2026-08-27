package com.flowforge.flowforge.dto;

import com.flowforge.flowforge.entity.WorkflowStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkflowSummaryResponse(
        UUID id,
        String name,
        WorkflowStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
