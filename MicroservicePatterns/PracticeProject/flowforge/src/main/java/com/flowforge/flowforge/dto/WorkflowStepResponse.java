package com.flowforge.flowforge.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkflowStepResponse(
        UUID id,
        String name,
        String type,
        int stepOrder,
        String config,
        Instant createdAt,
        Instant updatedAt
) {
}
