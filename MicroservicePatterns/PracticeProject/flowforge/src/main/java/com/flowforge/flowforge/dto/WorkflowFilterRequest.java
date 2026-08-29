package com.flowforge.flowforge.dto;

import com.flowforge.flowforge.entity.WorkflowStatus;

import java.time.Instant;

public record WorkflowFilterRequest(
        String search,
        WorkflowStatus status,
        Integer minRetries,
        Integer maxRetries,
        Instant createdAfter,
        Instant createdBefore
) {}
