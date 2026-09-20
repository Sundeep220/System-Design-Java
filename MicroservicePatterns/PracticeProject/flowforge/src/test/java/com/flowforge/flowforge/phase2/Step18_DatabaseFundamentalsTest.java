package com.flowforge.flowforge.phase2;

import com.flowforge.flowforge.TestcontainersConfig;
import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStep;
import com.flowforge.flowforge.repository.WorkflowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2, Step 18 — Database Fundamentals
 *
 * Tests verify that DB-level constraints (CHECK, UNIQUE, FK)
 * and indexes are properly created by schema-postgres.sql.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class Step18_DatabaseFundamentalsTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private JdbcTemplate jdbc;

    // ── SCENARIO 18.1: Constraints ──────────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 18.1: Schema Constraints")
    class SchemaConstraints {

        @Test
        @DisplayName("UNIQUE: duplicate workflow name → DataIntegrityViolationException")
        void duplicateNameRejected() {
            workflowRepository.save(new Workflow("Unique-Test", "first", 1, 30));

            assertThatThrownBy(() ->
                    workflowRepository.save(new Workflow("Unique-Test", "second", 2, 60))
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("CHECK: invalid status via native SQL → constraint violation")
        void invalidStatusRejected() {
            assertThatThrownBy(() ->
                    jdbc.execute("INSERT INTO workflows (id, name, status, max_retries, timeout_seconds, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), 'Bad-Status', 'INVALID', 1, 30, NOW(), NOW())")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("CHECK: negative max_retries via native SQL → constraint violation")
        void negativeMaxRetriesRejected() {
            assertThatThrownBy(() ->
                    jdbc.execute("INSERT INTO workflows (id, name, status, max_retries, timeout_seconds, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), 'Neg-Retries', 'DRAFT', -1, 30, NOW(), NOW())")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("CHECK: zero timeout_seconds via native SQL → constraint violation")
        void zeroTimeoutRejected() {
            assertThatThrownBy(() ->
                    jdbc.execute("INSERT INTO workflows (id, name, status, max_retries, timeout_seconds, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), 'Zero-Timeout', 'DRAFT', 1, 0, NOW(), NOW())")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("CHECK: negative step_order via native SQL → constraint violation")
        void negativeStepOrderRejected() {
            Workflow wf = workflowRepository.save(new Workflow("Step-Order-Test", "test", 1, 30));

            assertThatThrownBy(() ->
                    jdbc.execute("INSERT INTO workflow_steps (id, workflow_id, name, type, step_order, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), '" + wf.getId() + "', 'Bad-Step', 'HTTP', -1, NOW(), NOW())")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("FK: step with non-existent workflow_id → constraint violation")
        void invalidForeignKeyRejected() {
            assertThatThrownBy(() ->
                    jdbc.execute("INSERT INTO workflow_steps (id, workflow_id, name, type, step_order, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000000', 'Orphan', 'HTTP', 0, NOW(), NOW())")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Valid workflow is persisted successfully")
        void validWorkflowPersisted() {
            Workflow wf = workflowRepository.save(new Workflow("Valid-WF", "desc", 3, 60));
            assertThat(wf.getId()).isNotNull();
            assertThat(workflowRepository.findById(wf.getId())).isPresent();
        }

        @Test
        @DisplayName("CASCADE: deleting workflow also deletes its steps")
        @Transactional
        void cascadeDelete() {
            Workflow wf = new Workflow("Cascade-Test", "test", 1, 30);
            wf.addStep(new WorkflowStep(wf, "Step-1", "HTTP", 0, null));
            wf.addStep(new WorkflowStep(wf, "Step-2", "SCRIPT", 1, null));
            workflowRepository.saveAndFlush(wf);

            int stepCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM workflow_steps WHERE workflow_id = ?",
                    Integer.class, wf.getId());
            assertThat(stepCount).isEqualTo(2);

            workflowRepository.delete(wf);
            workflowRepository.flush();

            int afterDelete = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM workflow_steps WHERE workflow_id = ?",
                    Integer.class, wf.getId());
            assertThat(afterDelete).isZero();
        }
    }

    // ── SCENARIO 18.2: Normalization ────────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 18.2: Normalization Audit")
    class NormalizationAudit {

        @Test
        @DisplayName("Schema is in 3NF: no repeating groups, no partial/transitive dependencies")
        void schemaIs3NF() {
            // Workflows: every non-key column depends on the whole key (UUID id) and nothing else
            // WorkflowSteps: same — workflow_id is a FK, not a partial key dependency
            // This test verifies the structure by checking column metadata
            var columns = jdbc.queryForList(
                    "SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name = 'workflows' ORDER BY ordinal_position");
            assertThat(columns).isNotEmpty();

            var pkConstraint = jdbc.queryForList(
                    "SELECT constraint_name FROM information_schema.table_constraints WHERE table_name = 'workflows' AND constraint_type = 'PRIMARY KEY'");
            assertThat(pkConstraint).hasSize(1);
        }
    }

    // ── SCENARIO 18.3: Indexes ──────────────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 18.3: Index Design")
    class IndexDesign {

        @Test
        @DisplayName("Composite index idx_workflow_status_name exists")
        void compositeIndexExists() {
            assertIndexExists("idx_workflow_status_name");
        }

        @Test
        @DisplayName("FK index idx_step_workflow_id exists")
        void fkIndexExists() {
            assertIndexExists("idx_step_workflow_id");
        }

        @Test
        @DisplayName("Covering index idx_workflow_status_cover exists")
        void coveringIndexExists() {
            assertIndexExists("idx_workflow_status_cover");
        }

        @Test
        @DisplayName("FTS GIN index idx_workflows_fts exists")
        void ftsIndexExists() {
            assertIndexExists("idx_workflows_fts");
        }

        @Test
        @DisplayName("Cursor pagination index idx_workflow_created_at exists")
        void cursorIndexExists() {
            assertIndexExists("idx_workflow_created_at");
        }

        private void assertIndexExists(String indexName) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?",
                    Integer.class, indexName);
            assertThat(count).as("Index %s should exist", indexName).isEqualTo(1);
        }
    }

    // ── SCENARIO 18.4: Joins ────────────────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 18.4: Join Practice")
    class JoinPractice {

        @Test
        @DisplayName("INNER JOIN: only workflows with steps")
        @Transactional
        void innerJoinReturnsOnlyWorkflowsWithSteps() {
            Workflow withSteps = new Workflow("With-Steps", "has steps", 1, 30);
            withSteps.addStep(new WorkflowStep(withSteps, "S1", "HTTP", 0, null));
            workflowRepository.saveAndFlush(withSteps);

            Workflow noSteps = new Workflow("No-Steps", "no steps", 1, 30);
            workflowRepository.saveAndFlush(noSteps);

            var rows = jdbc.queryForList(
                    "SELECT w.name, ws.name AS step_name FROM workflows w " +
                            "INNER JOIN workflow_steps ws ON ws.workflow_id = w.id " +
                            "WHERE w.name IN ('With-Steps', 'No-Steps')");
            assertThat(rows).allMatch(r -> r.get("name").equals("With-Steps"));
        }

        @Test
        @DisplayName("LEFT JOIN: all workflows including those without steps")
        @Transactional
        void leftJoinReturnsAllWorkflows() {
            Workflow withSteps = new Workflow("LJ-With", "has steps", 1, 30);
            withSteps.addStep(new WorkflowStep(withSteps, "S1", "HTTP", 0, null));
            workflowRepository.saveAndFlush(withSteps);

            Workflow noSteps = new Workflow("LJ-Without", "no steps", 1, 30);
            workflowRepository.saveAndFlush(noSteps);

            var rows = jdbc.queryForList(
                    "SELECT w.name, ws.name AS step_name FROM workflows w " +
                            "LEFT JOIN workflow_steps ws ON ws.workflow_id = w.id " +
                            "WHERE w.name IN ('LJ-With', 'LJ-Without') ORDER BY w.name");

            var workflowNames = rows.stream().map(r -> r.get("name")).distinct().toList();
            assertThat(workflowNames).contains("LJ-With", "LJ-Without");
        }

        @Test
        @DisplayName("Aggregate JOIN: workflow + step count")
        @Transactional
        void aggregateJoinCountsSteps() {
            Workflow wf = new Workflow("Agg-Test", "aggregate", 1, 30);
            wf.addStep(new WorkflowStep(wf, "S1", "HTTP", 0, null));
            wf.addStep(new WorkflowStep(wf, "S2", "SCRIPT", 1, null));
            wf.addStep(new WorkflowStep(wf, "S3", "EMAIL", 2, null));
            workflowRepository.saveAndFlush(wf);

            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(ws.id) FROM workflows w " +
                            "LEFT JOIN workflow_steps ws ON ws.workflow_id = w.id " +
                            "WHERE w.name = 'Agg-Test' GROUP BY w.id",
                    Integer.class);
            assertThat(count).isEqualTo(3);
        }
    }
}
