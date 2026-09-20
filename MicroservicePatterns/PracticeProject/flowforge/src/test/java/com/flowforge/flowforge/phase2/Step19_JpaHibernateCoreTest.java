package com.flowforge.flowforge.phase2;

import com.flowforge.flowforge.TestcontainersConfig;
import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStatus;
import com.flowforge.flowforge.entity.WorkflowStep;
import com.flowforge.flowforge.repository.WorkflowRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2, Step 19 — JPA / Hibernate Core
 *
 * Integration tests against real PostgreSQL (via Testcontainers) covering:
 *   19.1  Entity mapping verification
 *   19.2  Persistence context & dirty checking
 *   19.3  Entity states (Managed, Detached, merge)
 *   19.4  persist vs merge vs save()
 *   19.5  find() vs getReference()
 *   19.6  Batch insert with flush/clear
 *   19.7  EntityManager query methods (JPQL, native, Criteria, getSingleResult)
 *   19.8  Bulk update staleness
 *
 * Watch the SQL logs (org.hibernate.SQL=DEBUG) while running these tests
 * to observe exactly what Hibernate does for each operation.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class Step19_JpaHibernateCoreTest {

    private static final Logger log = LoggerFactory.getLogger(Step19_JpaHibernateCoreTest.class);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private WorkflowRepository workflowRepository;

    // ── SCENARIO 19.1: Entity Mapping Verification ──────────────────────

    @Nested
    @DisplayName("SCENARIO 19.1: Entity Mapping")
    class EntityMapping {

        @Test
        @DisplayName("Workflow entity has UUID PK assigned on persist")
        @Transactional
        void workflowHasUuidPk() {
            Workflow wf = new Workflow("Mapping-Test-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();

            assertThat(wf.getId()).isNotNull();
            assertThat(wf.getId()).isInstanceOf(UUID.class);
            log.info("UUID assigned: {}", wf.getId());
        }

        @Test
        @DisplayName("WorkflowStep entity has UUID PK and FK to Workflow")
        @Transactional
        void stepHasUuidPkAndFk() {
            Workflow wf = new Workflow("Step-Mapping-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);

            WorkflowStep step = new WorkflowStep(wf, "Step-1", "HTTP", 0, null);
            em.persist(step);
            em.flush();

            assertThat(step.getId()).isNotNull();
            assertThat(step.getWorkflow().getId()).isEqualTo(wf.getId());
        }

        @Test
        @DisplayName("createdAt and updatedAt are set by @PrePersist")
        @Transactional
        void timestampsAreSet() {
            Workflow wf = new Workflow("Timestamp-Test-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();

            assertThat(wf.getCreatedAt()).isNotNull();
            assertThat(wf.getUpdatedAt()).isNotNull();
            assertThat(wf.getCreatedAt()).isEqualTo(wf.getUpdatedAt());
        }
    }

    // ── SCENARIO 19.2: Dirty Checking ───────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 19.2: Dirty Checking")
    class DirtyChecking {

        @Test
        @DisplayName("Modifying a MANAGED entity without save() triggers UPDATE on flush")
        @Transactional
        void dirtyCheckingTriggersUpdate() {
            // Persist a workflow
            Workflow wf = new Workflow("Dirty-Check-" + UUID.randomUUID(), "original", 1, 30);
            em.persist(wf);
            em.flush();

            UUID id = wf.getId();
            String newName = "Dirty-Updated-" + System.currentTimeMillis();

            // Modify — NO save() call
            wf.setName(newName);
            log.info("Changed name to '{}' — NO save() called. Dirty checking should handle it.", newName);

            // Flush forces the dirty check
            em.flush();

            // Clear the persistence context and re-load to verify
            em.clear();
            Workflow reloaded = em.find(Workflow.class, id);
            assertThat(reloaded.getName()).isEqualTo(newName);
            log.info("✅ Dirty checking worked: DB has name='{}'", reloaded.getName());
        }

        @Test
        @DisplayName("Dirty checking only fires for actual changes (no-op change → no UPDATE)")
        @Transactional
        void noOpChangeNoUpdate() {
            Workflow wf = new Workflow("NoOp-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();

            // Set the same value — this is a no-op
            wf.setName(wf.getName());
            // Hibernate compares snapshots — if nothing changed, no UPDATE is issued
            // Check logs: there should be NO UPDATE SQL here
            em.flush();
            log.info("✅ No UPDATE should appear in logs above (same value was set)");
        }
    }

    // ── SCENARIO 19.3: Entity States ────────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 19.3: Entity States")
    class EntityStates {

        @Test
        @DisplayName("find() → MANAGED, detach() → DETACHED, merge() → new MANAGED copy")
        @Transactional
        void entityStateTransitions() {
            // Persist
            Workflow wf = new Workflow("States-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();
            UUID id = wf.getId();

            // 1. After find() → MANAGED
            Workflow found = em.find(Workflow.class, id);
            assertThat(em.contains(found)).isTrue();
            log.info("After find(): managed={}", em.contains(found));

            // 2. detach() → DETACHED
            em.detach(found);
            assertThat(em.contains(found)).isFalse();
            log.info("After detach(): managed={}", em.contains(found));

            // 3. Change while detached → NOT tracked
            found.setName("Detached-Change");
            em.flush(); // no UPDATE should appear
            log.info("Changed name while DETACHED — no UPDATE should appear in logs above");

            // 4. merge() → returns NEW managed copy
            Workflow merged = em.merge(found);
            assertThat(em.contains(merged)).isTrue();
            assertThat(em.contains(found)).isFalse();
            assertThat(merged).isNotSameAs(found); // different objects!
            assertThat(merged.getName()).isEqualTo("Detached-Change");
            log.info("After merge(): original managed={}, merged managed={}, sameObject={}",
                    em.contains(found), em.contains(merged), found == merged);
        }

        @Test
        @DisplayName("New entity is TRANSIENT until persist()")
        @Transactional
        void transientState() {
            Workflow wf = new Workflow("Transient-" + UUID.randomUUID(), "desc", 1, 30);

            // Before persist — TRANSIENT (not managed)
            assertThat(em.contains(wf)).isFalse();
            log.info("Before persist: managed={}", em.contains(wf));

            em.persist(wf);

            // After persist — MANAGED
            assertThat(em.contains(wf)).isTrue();
            log.info("After persist: managed={}", em.contains(wf));
        }
    }

    // ── SCENARIO 19.4: persist vs merge vs save() ───────────────────────

    @Nested
    @DisplayName("SCENARIO 19.4: persist vs merge vs save()")
    class PersistMergeSave {

        @Test
        @DisplayName("persist(): assigns ID, one INSERT, no SELECT")
        @Transactional
        void persistOnlyInserts() {
            log.info("── persist() test — watch for ONE INSERT, no SELECT ──");
            Workflow wf = new Workflow("Persist-" + UUID.randomUUID(), "desc", 1, 30);

            assertThat(wf.getId()).isNull();
            em.persist(wf);
            assertThat(wf.getId()).isNotNull();

            em.flush();
            log.info("✅ persist() done. ID={}", wf.getId());
        }

        @Test
        @DisplayName("merge() on detached: SELECT + UPDATE in logs")
        @Transactional
        void mergeDetachedSelectsAndUpdates() {
            log.info("── merge() on detached — watch for SELECT + UPDATE ──");

            Workflow wf = new Workflow("Merge-Detach-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();
            UUID id = wf.getId();

            // Detach
            em.detach(wf);
            wf.setName("Merged-Name-" + System.currentTimeMillis());

            // Merge — triggers SELECT to re-load, then UPDATE for changes
            Workflow merged = em.merge(wf);
            em.flush();

            assertThat(merged.getName()).isEqualTo(wf.getName());
            log.info("✅ merge() done. Check logs for SELECT + UPDATE");
        }

        @Test
        @DisplayName("save() on MANAGED entity is unnecessary — dirty checking handles it")
        @Transactional
        void saveOnManagedIsUnnecessary() {
            log.info("── save() on managed — watch for UNNECESSARY operations ──");

            Workflow wf = new Workflow("Save-Managed-" + UUID.randomUUID(), "desc", 1, 30);
            workflowRepository.saveAndFlush(wf);
            UUID id = wf.getId();

            // Entity is MANAGED. setName triggers dirty checking automatically.
            wf.setName("Updated-Without-Save-" + System.currentTimeMillis());

            // save() is NOT needed here — it calls merge() internally, which is redundant
            // for an already-managed entity
            workflowRepository.save(wf); // UNNECESSARY!
            em.flush();

            log.info("✅ save() on managed entity — check if there are extra operations in logs");
            log.info("   Remove the save() call: dirty checking produces the same result");

            em.clear();
            Workflow reloaded = em.find(Workflow.class, id);
            assertThat(reloaded.getName()).startsWith("Updated-Without-Save-");
        }
    }

    // ── SCENARIO 19.5: find() vs getReference() ─────────────────────────

    @Nested
    @DisplayName("SCENARIO 19.5: find() vs getReference()")
    class FindVsGetReference {

        @Test
        @DisplayName("getReference() returns proxy — no SELECT until property access")
        @Transactional
        void getReferenceReturnsProxy() {
            Workflow wf = new Workflow("Ref-Test-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();
            em.clear(); // clear so getReference has nothing in cache

            log.info("── getReference() — should NOT issue SELECT ──");
            Workflow proxy = em.getReference(Workflow.class, wf.getId());

            // proxy class is a Hibernate-generated subclass, not Workflow itself
            assertThat(proxy.getClass()).isNotEqualTo(Workflow.class);
            log.info("Proxy class: {}", proxy.getClass().getSimpleName());

            // Accessing getId() does NOT trigger SELECT (ID is already known)
            assertThat(proxy.getId()).isEqualTo(wf.getId());

            // Accessing getName() DOES trigger SELECT (lazy initialization)
            log.info("── Accessing getName() — NOW the SELECT fires ──");
            assertThat(proxy.getName()).isEqualTo(wf.getName());
        }

        @Test
        @DisplayName("getReference() for FK: INSERT step without SELECT for workflow")
        @Transactional
        void getReferenceForFkAvoidSelect() {
            Workflow wf = new Workflow("FK-Ref-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();
            em.clear();

            log.info("── getReference() to set FK — only INSERT, no SELECT ──");
            Workflow proxy = em.getReference(Workflow.class, wf.getId());
            WorkflowStep step = new WorkflowStep(proxy, "Proxy-Step", "HTTP", 0, null);
            em.persist(step);
            em.flush();

            assertThat(step.getId()).isNotNull();
            log.info("✅ Step persisted with workflow FK via proxy. Check logs: no SELECT for workflow");
        }

        @Test
        @DisplayName("find() for FK: SELECT for workflow + INSERT for step (one extra query)")
        @Transactional
        void findForFkExtraSelect() {
            Workflow wf = new Workflow("FK-Find-" + UUID.randomUUID(), "desc", 1, 30);
            em.persist(wf);
            em.flush();
            em.clear();

            log.info("── find() to set FK — SELECT + INSERT ──");
            Workflow found = em.find(Workflow.class, wf.getId());
            WorkflowStep step = new WorkflowStep(found, "Find-Step", "HTTP", 0, null);
            em.persist(step);
            em.flush();

            assertThat(step.getId()).isNotNull();
            log.info("✅ Step persisted. Check logs: SELECT for workflow + INSERT for step");
        }
    }

    // ── SCENARIO 19.6: Batch Insert with flush/clear ────────────────────

    @Nested
    @DisplayName("SCENARIO 19.6: Batch Insert")
    class BatchInsert {

        @Test
        @DisplayName("Batch insert with flush/clear every 50 — keeps persistence context small")
        @Transactional
        void batchInsertWithFlushClear() {
            int count = 200;
            int batchSize = 50;
            long start = System.currentTimeMillis();
            String prefix = "Batch-FC-" + System.currentTimeMillis() + "-";

            for (int i = 1; i <= count; i++) {
                Workflow wf = new Workflow(prefix + i, "batch test", 1, 30);
                em.persist(wf);

                if (i % batchSize == 0) {
                    em.flush();
                    em.clear();
                    log.info("Flushed & cleared at i={}", i);
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("✅ Batch insert: {} entities with flush/clear in {}ms", count, elapsed);
        }

        @Test
        @DisplayName("Batch insert WITHOUT flush/clear — all entities stay in context")
        @Transactional
        void batchInsertWithoutFlushClear() {
            int count = 200;
            long start = System.currentTimeMillis();
            String prefix = "Batch-NFC-" + System.currentTimeMillis() + "-";

            for (int i = 1; i <= count; i++) {
                Workflow wf = new Workflow(prefix + i, "batch test", 1, 30);
                em.persist(wf);
            }
            em.flush();

            long elapsed = System.currentTimeMillis() - start;
            log.info("✅ Batch insert: {} entities WITHOUT flush/clear in {}ms", count, elapsed);
            log.info("   All {} entities are still in the persistence context", count);
        }
    }

    // ── SCENARIO 19.7: Query Methods ────────────────────────────────────

    @Nested
    @DisplayName("SCENARIO 19.7: EntityManager Query Methods")
    class QueryMethods {

        private String testPrefix;

        @BeforeEach
        void setUp() {
            testPrefix = "QM-" + System.currentTimeMillis() + "-";
        }

        @Test
        @DisplayName("JPQL, native SQL, and Criteria API all return MANAGED entities")
        @Transactional
        void allQueryMethodsReturnManagedEntities() {
            Workflow wf = new Workflow(testPrefix + "Active", "query test", 1, 30);
            wf.setStatus(WorkflowStatus.ACTIVE);
            em.persist(wf);
            em.flush();
            em.clear();

            // 1. JPQL
            log.info("── JPQL query ──");
            List<Workflow> jpql = em.createQuery(
                    "SELECT w FROM Workflow w WHERE w.status = :status AND w.name = :name", Workflow.class)
                    .setParameter("status", WorkflowStatus.ACTIVE)
                    .setParameter("name", testPrefix + "Active")
                    .getResultList();
            assertThat(jpql).hasSize(1);
            assertThat(em.contains(jpql.getFirst())).isTrue();
            log.info("JPQL: managed={}", em.contains(jpql.getFirst()));

            em.clear();

            // 2. Native SQL
            log.info("── Native SQL query ──");
            @SuppressWarnings("unchecked")
            List<Workflow> nativeResults = em.createNativeQuery(
                    "SELECT * FROM workflows WHERE status = :status AND name = :name", Workflow.class)
                    .setParameter("status", "ACTIVE")
                    .setParameter("name", testPrefix + "Active")
                    .getResultList();
            assertThat(nativeResults).hasSize(1);
            assertThat(em.contains(nativeResults.getFirst())).isTrue();
            log.info("Native: managed={}", em.contains(nativeResults.getFirst()));

            em.clear();

            // 3. Criteria API
            log.info("── Criteria API query ──");
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Workflow> cq = cb.createQuery(Workflow.class);
            Root<Workflow> root = cq.from(Workflow.class);
            cq.where(
                    cb.equal(root.get("status"), WorkflowStatus.ACTIVE),
                    cb.equal(root.get("name"), testPrefix + "Active")
            );
            List<Workflow> criteria = em.createQuery(cq).getResultList();
            assertThat(criteria).hasSize(1);
            assertThat(em.contains(criteria.getFirst())).isTrue();
            log.info("Criteria: managed={}", em.contains(criteria.getFirst()));
        }

        @Test
        @DisplayName("getSingleResult() with 0 results → NoResultException")
        @Transactional
        void getSingleResultNoResults() {
            TypedQuery<Workflow> query = em.createQuery(
                    "SELECT w FROM Workflow w WHERE w.name = :name", Workflow.class);
            query.setParameter("name", "NONEXISTENT-" + UUID.randomUUID());

            assertThatThrownBy(query::getSingleResult)
                    .isInstanceOf(NoResultException.class);
            log.info("✅ getSingleResult() with 0 results → NoResultException");
        }

        @Test
        @DisplayName("getSingleResult() with 2+ results → NonUniqueResultException")
        @Transactional
        void getSingleResultMultipleResults() {
            // Insert two ACTIVE workflows
            Workflow wf1 = new Workflow(testPrefix + "Multi1", "test", 1, 30);
            wf1.setStatus(WorkflowStatus.ACTIVE);
            Workflow wf2 = new Workflow(testPrefix + "Multi2", "test", 1, 30);
            wf2.setStatus(WorkflowStatus.ACTIVE);
            em.persist(wf1);
            em.persist(wf2);
            em.flush();

            TypedQuery<Workflow> query = em.createQuery(
                    "SELECT w FROM Workflow w WHERE w.status = :status AND w.name LIKE :prefix", Workflow.class);
            query.setParameter("status", WorkflowStatus.ACTIVE);
            query.setParameter("prefix", testPrefix + "Multi%");

            assertThatThrownBy(query::getSingleResult)
                    .isInstanceOf(NonUniqueResultException.class);
            log.info("✅ getSingleResult() with 2+ results → NonUniqueResultException");
        }
    }

    // ── SCENARIO 19.8: Bulk Update Staleness ────────────────────────────

    @Nested
    @DisplayName("SCENARIO 19.8: Bulk Update")
    class BulkUpdate {

        @Test
        @DisplayName("Bulk UPDATE bypasses persistence context — loaded entity becomes STALE")
        @Transactional
        void bulkUpdateCausesStaleness() {
            // Create a DRAFT workflow with old createdAt
            Workflow wf = new Workflow("Stale-Test-" + UUID.randomUUID(), "old workflow", 1, 30);
            em.persist(wf);
            em.flush();

            // Manually set createdAt to 60 days ago via native SQL
            em.createNativeQuery("UPDATE workflows SET created_at = :date WHERE id = :id")
                    .setParameter("date", Instant.now().minus(60, ChronoUnit.DAYS))
                    .setParameter("id", wf.getId())
                    .executeUpdate();

            // The entity is still MANAGED with status=DRAFT
            assertThat(wf.getStatus()).isEqualTo(WorkflowStatus.DRAFT);
            assertThat(em.contains(wf)).isTrue();

            // Bulk JPQL UPDATE — bypasses the persistence context entirely
            log.info("── Bulk UPDATE — one SQL, no entity loading ──");
            int updated = em.createQuery(
                    "UPDATE Workflow w SET w.status = :newStatus WHERE w.createdAt < :cutoff AND w.status <> :newStatus")
                    .setParameter("newStatus", WorkflowStatus.ARCHIVED)
                    .setParameter("cutoff", Instant.now().minus(30, ChronoUnit.DAYS))
                    .executeUpdate();

            log.info("Bulk update: {} rows archived", updated);
            assertThat(updated).isGreaterThanOrEqualTo(1);

            // ⚠️ STALE: the managed entity still has the OLD status
            assertThat(wf.getStatus()).isEqualTo(WorkflowStatus.DRAFT);
            log.info("⚠️ STALE: in-memory status={} but DB status=ARCHIVED", wf.getStatus());

            // Fix: refresh from DB
            em.refresh(wf);
            assertThat(wf.getStatus()).isEqualTo(WorkflowStatus.ARCHIVED);
            log.info("✅ After refresh(): status={}", wf.getStatus());
        }

        @Test
        @DisplayName("Bulk UPDATE returns affected row count without loading entities")
        @Transactional
        void bulkUpdateReturnsCount() {
            String prefix = "Bulk-" + System.currentTimeMillis() + "-";
            for (int i = 0; i < 5; i++) {
                Workflow wf = new Workflow(prefix + i, "bulk", 1, 30);
                wf.setStatus(WorkflowStatus.ACTIVE);
                em.persist(wf);
            }
            em.flush();
            em.clear();

            log.info("── Bulk UPDATE — watch: one SQL, no SELECT for entities ──");
            int updated = em.createQuery(
                    "UPDATE Workflow w SET w.status = :newStatus WHERE w.name LIKE :prefix")
                    .setParameter("newStatus", WorkflowStatus.PAUSED)
                    .setParameter("prefix", prefix + "%")
                    .executeUpdate();

            assertThat(updated).isEqualTo(5);
            log.info("✅ {} rows updated with one SQL statement", updated);
        }
    }
}
