package com.flowforge.flowforge.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Step 21, Scenario 21.6 — Listens for workflow events after TX commit/rollback.
 */
@Component
public class WorkflowEventListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowCreatedCommit(WorkflowCreatedEvent event) {
        log.info("✅ AFTER_COMMIT: Workflow committed: {}", event.workflowId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onWorkflowCreatedRollback(WorkflowCreatedEvent event) {
        log.info("❌ AFTER_ROLLBACK: Workflow creation rolled back: {}", event.workflowId());
    }
}
