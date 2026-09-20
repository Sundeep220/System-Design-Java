package com.flowforge.flowforge.dto;

import java.util.UUID;

/**
 * Step 20, Scenario 20.7 — Interface-based DTO Projection.
 *
 * Spring Data auto-generates a proxy implementing this interface.
 * Only the columns matching getter names are SELECTed — no entity loaded.
 */
public interface WorkflowProjection {
    UUID getId();
    String getName();
    String getStatus();
}
