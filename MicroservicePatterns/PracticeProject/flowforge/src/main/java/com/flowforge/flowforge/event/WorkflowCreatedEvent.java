package com.flowforge.flowforge.event;

import java.util.UUID;

/**
 * Step 21, Scenario 21.6 — Published after a workflow is created.
 * Consumed by @TransactionalEventListener to fire only after commit.
 */
public record WorkflowCreatedEvent(UUID workflowId) {
}
