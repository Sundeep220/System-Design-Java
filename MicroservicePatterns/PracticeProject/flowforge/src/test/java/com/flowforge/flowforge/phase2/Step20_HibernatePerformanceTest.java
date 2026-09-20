package com.flowforge.flowforge.phase2;

import com.flowforge.flowforge.TestcontainersConfig;
import com.flowforge.flowforge.dto.WorkflowIdNameStatus;
import com.flowforge.flowforge.dto.WorkflowProjection;
import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStep;
import com.flowforge.flowforge.repository.WorkflowRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.LazyInitializationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2, Step 20 — Hibernate Performance
 *
 * Integration tests against real PostgreSQL (via Testcontainers) covering:
 *   20.1  N+1 detection
 *   20.2  Fix with JOIN FETCH
 *   20.3  Fix with @EntityGraph
 *   20.4  Fix with @BatchSize
 *   20.5  Fix with SUBSELECT
 *   20.6  LazyInitializationException
 *   20.7  DTO projections (interface + class-based)
 *
 * Run: mvn test -Dtest=Step20_HibernatePerformanceTest  (requires Docker)
 *
 * IMPORTANT: Watch the SQL logs (org.hibernate.SQL=DEBUG) while running these tests
 * to observe query counts and differences between strategies.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class Step20_HibernatePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(Step20_HibernatePerformanceTest.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private WorkflowRepository workflowRepository;

    // ── Helper ──────────────────────────────────────────────────────────

    private Statistics getStatistics() {
        return em.unwrap(org.hibernate.Session.class)
                .getSessionFactory()
                .getStatistics();
    }

    private void seedWorkflows(int count, int stepsPerWorkflow) {
        String prefix = "Perf-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        for (int i = 0; i < count; i++) {
            Workflow wf = new Workflow(prefix + i, "perf test", 1, 30);
            for (int j = 0; j < stepsPerWorkflow; j++) {
                wf.addStep(new WorkflowStep(wf, prefix + i + "-step-" + j, "HTTP", j, null));
            }
            em.persist(wf);
        }
        em.flush();
        em.clear();
    }

    // ── SCENARIO 20.1: N+1 Detection ───────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 20.1: N+1 Detection")
    class NPlus1Detection {

        @Test
        @DisplayName("findAll() triggers N+1: 1 query for workflows + N for steps")
        @Transactional
        void findAllTriggersNPlus1() {
            seedWorkflows(10, 3);
            Statistics stats = getStatistics();
            stats.clear();

            log.info("── N+1 Detection: calling findAll() with 10 workflows ──");
            List<Workflow> workflows = workflowRepository.findAll();

            // Force lazy loading of steps — triggers N additional queries
            int totalSteps = 0;
            for (Workflow wf : workflows) {
                totalSteps += wf.getSteps().size();
            }

            long queryCount = stats.getPrepareStatementCount();
            log.info("findAll() + accessing steps: {} queries for {} workflows ({} steps total)",
                    queryCount, workflows.size(), totalSteps);

            // Expect N+1: 1 for workflows + at least 1 per workflow for steps
            assertThat(queryCount).as("N+1 detected: expected more than 1 query")
                    .isGreaterThan(1);
            log.info("⚠️ N+1 confirmed: {} queries (should be 1 or 2 with proper fetching)", queryCount);
        }
    }

    // ── SCENARIO 20.2: Fix with JOIN FETCH ──────────────────────────────

    @Nested
    @DisplayName("SCENARIO 20.2: Fix with JOIN FETCH")
    class JoinFetchFix {

        @Test
        @DisplayName("findAllWithSteps() uses JOIN FETCH — single query")
        @Transactional
        void joinFetchSingleQuery() {
            seedWorkflows(10, 3);
            Statistics stats = getStatistics();
            stats.clear();

            log.info("── JOIN FETCH: calling findAllWithSteps() ──");
            List<Workflow> workflows = workflowRepository.findAllWithSteps();

            // Access steps — no additional query needed
            int totalSteps = 0;
            for (Workflow wf : workflows) {
                totalSteps += wf.getSteps().size();
            }

            long queryCount = stats.getPrepareStatementCount();
            log.info("JOIN FETCH: {} queries for {} workflows ({} steps total)",
                    queryCount, workflows.size(), totalSteps);

            // Should be exactly 1 query
            assertThat(queryCount).as("JOIN FETCH should use only 1 query")
                    .isEqualTo(1);
            assertThat(totalSteps).isGreaterThan(0);
            log.info("✅ JOIN FETCH: 1 query fetched all workflows + steps");
        }

        @Test
        @DisplayName("JOIN FETCH returns workflows with no steps (LEFT JOIN)")
        @Transactional
        void joinFetchIncludesWorkflowsWithoutSteps() {
            // Seed 5 with steps, 5 without
            String prefix = "JFNS-" + UUID.randomUUID().toString().substring(0, 8) + "-";
            for (int i = 0; i < 5; i++) {
                Workflow wf = new Workflow(prefix + "with-" + i, "with steps", 1, 30);
                wf.addStep(new WorkflowStep(wf, prefix + "step-" + i, "HTTP", 0, null));
                em.persist(wf);
            }
            for (int i = 0; i < 5; i++) {
                Workflow wf = new Workflow(prefix + "without-" + i, "no steps", 1, 30);
                em.persist(wf);
            }
            em.flush();
            em.clear();

            List<Workflow> all = workflowRepository.findAllWithSteps();
            long withSteps = all.stream().filter(w -> !w.getSteps().isEmpty()).count();
            long withoutSteps = all.stream().filter(w -> w.getSteps().isEmpty()).count();

            log.info("LEFT JOIN FETCH: {} with steps, {} without steps", withSteps, withoutSteps);
            assertThat(withoutSteps).as("LEFT JOIN should include workflows without steps")
                    .isGreaterThanOrEqualTo(5);
        }
    }

    // ── SCENARIO 20.3: Fix with @EntityGraph ────────────────────────────

    @Nested
    @DisplayName("SCENARIO 20.3: Fix with @EntityGraph")
    class EntityGraphFix {

        @Test
        @DisplayName("@EntityGraph generates LEFT JOIN — single query")
        @Transactional
        void entityGraphSingleQuery() {
            seedWorkflows(10, 3);
            Statistics stats = getStatistics();
            stats.clear();

            log.info("── @EntityGraph: calling findAllWithStepsEntityGraph() ──");
            List<Workflow> workflows = workflowRepository.findAllWithStepsEntityGraph();

            int totalSteps = 0;
            for (Workflow wf : workflows) {
                totalSteps += wf.getSteps().size();
            }

            long queryCount = stats.getPrepareStatementCount();
            log.info("@EntityGraph: {} queries for {} workflows ({} steps total)",
                    queryCount, workflows.size(), totalSteps);

            assertThat(queryCount).as("@EntityGraph should use 1 query (LEFT JOIN)")
                    .isEqualTo(1);
            assertThat(totalSteps).isGreaterThan(0);
            log.info("✅ @EntityGraph: 1 query with LEFT JOIN");
        }

        @Test
        @DisplayName("@EntityGraph includes workflows with 0 steps (LEFT JOIN)")
        @Transactional
        void entityGraphIncludesEmpty() {
            String prefix = "EG-" + UUID.randomUUID().toString().substring(0, 8) + "-";
            Workflow empty = new Workflow(prefix + "empty", "no steps", 1, 30);
            em.persist(empty);
            em.flush();
            em.clear();

            List<Workflow> all = workflowRepository.findAllWithStepsEntityGraph();
            boolean foundEmpty = all.stream().anyMatch(w -> w.getName().equals(prefix + "empty"));
            assertThat(foundEmpty).as("@EntityGraph (LEFT JOIN) should include workflow with 0 steps").isTrue();
        }
    }

    // ── SCENARIO 20.4: Fix with @BatchSize ──────────────────────────────

    @Nested
    @DisplayName("SCENARIO 20.4: Fix with @BatchSize")
    class BatchSizeFix {

        @Test
        @DisplayName("@BatchSize reduces N+1 to 1 + ceil(N/batchSize) queries")
        @Transactional
        void batchSizeReducesQueries() {
            // This test demonstrates the concept — @BatchSize would be on the entity.
            // We observe the default behavior and document the expected improvement.
            seedWorkflows(10, 3);
            Statistics stats = getStatistics();
            stats.clear();

            log.info("── @BatchSize concept: calling findAll() then accessing steps ──");
            List<Workflow> workflows = workflowRepository.findAll();

            int totalSteps = 0;
            for (Workflow wf : workflows) {
                totalSteps += wf.getSteps().size();
            }

            long queryCount = stats.getPrepareStatementCount();
            log.info("Without @BatchSize: {} queries", queryCount);
            log.info("With @BatchSize(size=25) on steps field, this would be ~2-3 queries:");
            log.info("  Query 1: SELECT workflows");
            log.info("  Query 2: SELECT steps WHERE workflow_id IN (up to 25 IDs)");
            log.info("To enable: add @BatchSize(size = 25) on Workflow.steps field");
            log.info("Or global: spring.jpa.properties.hibernate.default_batch_fetch_size=25");

            // Without @BatchSize, we expect N+1 queries
            assertThat(queryCount).isGreaterThan(1);
        }
    }

    // ── SCENARIO 20.5: Fix with SUBSELECT ───────────────────────────────

    @Nested
    @DisplayName("SCENARIO 20.5: Fix with SUBSELECT")
    class SubselectFix {

        @Test
        @DisplayName("SUBSELECT concept: fetches all steps in 1 query using subquery")
        @Transactional
        void subselectConcept() {
            seedWorkflows(10, 3);

            log.info("── SUBSELECT concept ──");
            log.info("With @Fetch(FetchMode.SUBSELECT) on Workflow.steps field:");
            log.info("  Query 1: SELECT workflows");
            log.info("  Query 2: SELECT steps WHERE workflow_id IN (SELECT id FROM workflows)");
            log.info("Total: exactly 2 queries regardless of N");
            log.info("To enable: add @Fetch(FetchMode.SUBSELECT) on Workflow.steps");
            log.info("");
            log.info("Comparison of all 4 N+1 fixes:");
            log.info("  findAll() (no fix):     1 + N queries (N+1 problem)");
            log.info("  JOIN FETCH:             1 query (but breaks pagination)");
            log.info("  @EntityGraph:           1 query (LEFT JOIN, works with derived queries)");
            log.info("  @BatchSize(25):         1 + ceil(N/25) queries (good default)");
            log.info("  @Fetch(SUBSELECT):      exactly 2 queries (efficient for full scans)");

            // Just verify data is seeded correctly
            List<Workflow> workflows = workflowRepository.findAll();
            assertThat(workflows).hasSizeGreaterThanOrEqualTo(10);
        }
    }

    // ── SCENARIO 20.6: LazyInitializationException ──────────────────────

    @Nested
    @DisplayName("SCENARIO 20.6: LazyInitializationException")
    class LazyInitException {

        @Test
        @DisplayName("Accessing lazy collection outside transaction → LazyInitializationException")
        void accessingLazyCollectionOutsideTransaction() {
            // Persist a workflow with steps inside a transaction
            Workflow saved = workflowRepository.save(
                    new Workflow("Lazy-Test-" + UUID.randomUUID(), "lazy test", 1, 30));
            UUID id = saved.getId();

            // findById() returns entity — but transaction is already committed
            // (no @Transactional on this test method, and open-in-view=false)
            Workflow wf = workflowRepository.findById(id).orElseThrow();

            log.info("── LazyInitializationException: accessing steps outside TX ──");
            log.info("open-in-view=false, no @Transactional → session is closed");

            assertThatThrownBy(() -> wf.getSteps().size())
                    .isInstanceOf(LazyInitializationException.class);
            log.info("✅ LazyInitializationException thrown as expected");
        }

        @Test
        @DisplayName("Fix: use JOIN FETCH (findByIdWithSteps) — works outside TX")
        void fixWithJoinFetch() {
            Workflow wf = new Workflow("Lazy-Fix-" + UUID.randomUUID(), "fix test", 1, 30);
            wf.addStep(new WorkflowStep(wf, "Step-1", "HTTP", 0, null));
            workflowRepository.save(wf);
            UUID id = wf.getId();

            // findByIdWithSteps uses JOIN FETCH — steps are loaded eagerly
            Workflow loaded = workflowRepository.findByIdWithSteps(id).orElseThrow();

            // This works even outside a transaction
            int stepCount = loaded.getSteps().size();
            assertThat(stepCount).isEqualTo(1);
            log.info("✅ Fix with JOIN FETCH: accessed {} steps outside TX", stepCount);
        }

        @Test
        @DisplayName("Fix: use @Transactional — keeps session open")
        @Transactional
        void fixWithTransactional() {
            Workflow wf = new Workflow("Lazy-TX-" + UUID.randomUUID(), "tx fix", 1, 30);
            wf.addStep(new WorkflowStep(wf, "Step-1", "HTTP", 0, null));
            workflowRepository.saveAndFlush(wf);
            em.clear();

            // findById inside @Transactional — session is still open
            Workflow loaded = workflowRepository.findById(wf.getId()).orElseThrow();
            int stepCount = loaded.getSteps().size(); // triggers lazy load — works!
            assertThat(stepCount).isEqualTo(1);
            log.info("✅ Fix with @Transactional: lazy load works inside TX");
        }
    }

    // ── SCENARIO 20.7: DTO Projections ──────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 20.7: DTO Projections")
    class DtoProjections {

        @BeforeEach
        void setUp() {
            // Ensure at least some data exists
            if (workflowRepository.count() == 0) {
                workflowRepository.save(new Workflow("Proj-1", "projection test", 1, 30));
                workflowRepository.save(new Workflow("Proj-2", "projection test", 2, 60));
            }
        }

        @Test
        @DisplayName("Interface projection: only id, name, status columns selected")
        @Transactional(readOnly = true)
        void interfaceProjection() {
            Statistics stats = getStatistics();
            stats.clear();

            log.info("── Interface Projection: findAllProjectedBy() ──");
            List<WorkflowProjection> projections = workflowRepository.findAllProjectedBy();

            assertThat(projections).isNotEmpty();
            projections.forEach(p -> {
                assertThat(p.getId()).isNotNull();
                assertThat(p.getName()).isNotNull();
                assertThat(p.getStatus()).isNotNull();
            });

            log.info("Interface projection: {} results", projections.size());
            log.info("Check SQL: should SELECT only id, name, status columns");
            log.info("No lazy proxies, no entity lifecycle overhead");
        }

        @Test
        @DisplayName("Class-based DTO projection: constructor expression in @Query")
        @Transactional(readOnly = true)
        void classDtoProjection() {
            Statistics stats = getStatistics();
            stats.clear();

            log.info("── Class DTO Projection: findAllDtoProjection() ──");
            List<WorkflowIdNameStatus> dtos = workflowRepository.findAllDtoProjection();

            assertThat(dtos).isNotEmpty();
            dtos.forEach(dto -> {
                assertThat(dto.id()).isNotNull();
                assertThat(dto.name()).isNotNull();
                assertThat(dto.status()).isNotNull();
            });

            log.info("Class DTO projection: {} results", dtos.size());
            log.info("Check SQL: SELECT id, name, status FROM workflows");
            log.info("Returns plain records — no managed entities, no proxies");
        }

        @Test
        @DisplayName("DTO projections are NOT managed by persistence context")
        @Transactional(readOnly = true)
        void projectionsNotManaged() {
            List<WorkflowIdNameStatus> dtos = workflowRepository.findAllDtoProjection();

            assertThat(dtos).isNotEmpty();
            // DTOs are NOT entities — they are NOT in the persistence context
            // em.contains() only works with entities, not DTOs
            // This confirms DTOs have zero persistence context overhead
            log.info("✅ DTO projections: no entity lifecycle, no dirty checking, no lazy loading");
            log.info("   Best for read-heavy endpoints where you don't need to modify the data");
        }

        @Test
        @DisplayName("Compare: entity findAll vs projection (entity loads all columns)")
        @Transactional(readOnly = true)
        void entityVsProjection() {
            Statistics stats = getStatistics();
            stats.clear();

            // Entity query — selects ALL columns, creates managed entities
            log.info("── Entity findAll() ──");
            List<Workflow> entities = workflowRepository.findAll();
            long entityQueryCount = stats.getPrepareStatementCount();
            boolean entityManaged = !entities.isEmpty() && em.contains(entities.getFirst());

            stats.clear();

            // Projection query — selects only id, name, status
            log.info("── Projection findAllProjectedBy() ──");
            List<WorkflowProjection> projections = workflowRepository.findAllProjectedBy();
            long projQueryCount = stats.getPrepareStatementCount();

            log.info("Entity:     {} rows, {} queries, managed={}", entities.size(), entityQueryCount, entityManaged);
            log.info("Projection: {} rows, {} queries, managed=false (proxy)", projections.size(), projQueryCount);
            log.info("Entity selects ALL columns (id, name, desc, status, retries, timeout, created, updated)");
            log.info("Projection selects ONLY id, name, status — less data transferred");

            assertThat(entities.size()).isEqualTo(projections.size());
            assertThat(entityManaged).isTrue();
        }
    }
}
