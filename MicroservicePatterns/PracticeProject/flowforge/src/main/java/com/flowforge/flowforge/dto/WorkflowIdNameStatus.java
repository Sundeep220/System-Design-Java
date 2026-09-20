package com.flowforge.flowforge.dto;

import com.flowforge.flowforge.entity.WorkflowStatus;

import java.util.UUID;

/**
 * Step 20, Scenario 20.7 — DTO Projection for read-only queries.
 *
 * Used as both:
 *  - Interface projection: Spring Data auto-generates the impl from getter names
 *  - Class-based DTO: used with constructor expression in @Query
 *
 * Only selects id, name, status — no steps collection, no lazy proxies.
 */
public record WorkflowIdNameStatus(
        UUID id,
        String name,
        WorkflowStatus status
) {
}
