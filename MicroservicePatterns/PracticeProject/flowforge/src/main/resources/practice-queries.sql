-- =====================================================================
-- FlowForge Practice Queries — Phase 2, Step 18: Database Fundamentals
-- =====================================================================
-- Run these queries manually in psql or DataGrip against the flowforge DB.
-- They are NOT executed by Spring Boot — this file is for learning only.
-- =====================================================================


-- ─────────────────────────────────────────────────────────────────────
-- SETUP: Insert test data (100 workflows with 0-5 steps each)
-- ─────────────────────────────────────────────────────────────────────

-- Run this once to populate test data for EXPLAIN ANALYZE experiments.
-- Generates 100 workflows in random statuses with 0-5 steps each.

INSERT INTO workflows (id, name, description, status, max_retries, timeout_seconds, created_at, updated_at)
SELECT
    gen_random_uuid(),
    'Workflow-' || i,
    'Description for workflow ' || i,
    (ARRAY['DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'])[1 + (random() * 3)::int],
    (random() * 5)::int,
    30 + (random() * 270)::int,
    NOW() - (random() * interval '90 days'),
    NOW()
FROM generate_series(1, 100) AS i
ON CONFLICT (name) DO NOTHING;  -- skip if already exists (idempotent)

-- Insert 0-5 steps per workflow
INSERT INTO workflow_steps (id, workflow_id, name, type, step_order, config, created_at, updated_at)
SELECT
    gen_random_uuid(),
    w.id,
    'Step-' || s,
    (ARRAY['HTTP', 'SCRIPT', 'EMAIL', 'DELAY', 'CONDITION'])[1 + (random() * 4)::int],
    s,
    '{"timeout": ' || (10 + (random() * 50)::int) || '}',
    w.created_at,
    NOW()
FROM workflows w
CROSS JOIN generate_series(1, (random() * 5 + 1)::int) AS s
WHERE NOT EXISTS (
    SELECT 1 FROM workflow_steps ws WHERE ws.workflow_id = w.id AND ws.step_order = s
);


-- ─────────────────────────────────────────────────────────────────────
-- SCENARIO 18.3: Index Design — EXPLAIN ANALYZE experiments
-- ─────────────────────────────────────────────────────────────────────

-- Test 1: Composite index (status, name) — leftmost prefix
-- EXPECT: Index Scan using idx_workflow_status_name
EXPLAIN ANALYZE SELECT * FROM workflows WHERE status = 'ACTIVE';

-- Test 2: Composite index — name only (NOT leftmost)
-- EXPECT: Seq Scan or Bitmap Scan (index NOT used for name-only filter)
EXPLAIN ANALYZE SELECT * FROM workflows WHERE name = 'Workflow-42';

-- Test 3: Composite index — both columns
-- EXPECT: Index Scan using idx_workflow_status_name
EXPLAIN ANALYZE SELECT * FROM workflows WHERE status = 'ACTIVE' AND name LIKE 'Workflow-1%';

-- Test 4: Covering index — Index Only Scan
-- EXPECT: Index Only Scan using idx_workflow_status_cover (no heap fetch)
EXPLAIN ANALYZE SELECT id, name FROM workflows WHERE status = 'ACTIVE';

-- Test 5: FK index — join performance
-- EXPECT: Index Scan on idx_step_workflow_id (not Seq Scan)
EXPLAIN ANALYZE
SELECT w.name, ws.name AS step_name, ws.step_order
FROM workflows w
JOIN workflow_steps ws ON ws.workflow_id = w.id
WHERE w.status = 'ACTIVE';

-- Test 6: Cursor pagination index
-- EXPECT: Index Scan Backward using idx_workflow_created_at
EXPLAIN ANALYZE
SELECT * FROM workflows
ORDER BY created_at DESC, id DESC
LIMIT 20;


-- ─────────────────────────────────────────────────────────────────────
-- SCENARIO 18.4: Join Practice
-- ─────────────────────────────────────────────────────────────────────

-- INNER JOIN: only workflows that have steps
-- Workflows with 0 steps are EXCLUDED
SELECT w.id, w.name, w.status, ws.name AS step_name, ws.type, ws.step_order
FROM workflows w
INNER JOIN workflow_steps ws ON ws.workflow_id = w.id
ORDER BY w.name, ws.step_order;

-- LEFT JOIN: ALL workflows + their steps (including workflows with 0 steps)
-- Workflows with no steps appear with NULL step columns
SELECT w.id, w.name, w.status, ws.name AS step_name, ws.type, ws.step_order
FROM workflows w
LEFT JOIN workflow_steps ws ON ws.workflow_id = w.id
ORDER BY w.name, ws.step_order;

-- LEFT JOIN: find workflows with NO steps (orphans)
SELECT w.id, w.name, w.status
FROM workflows w
LEFT JOIN workflow_steps ws ON ws.workflow_id = w.id
WHERE ws.id IS NULL;

-- Self-join: find workflows created on the same day
-- (matches each pair once, excludes self-matches)
SELECT
    w1.name AS workflow_a,
    w2.name AS workflow_b,
    DATE(w1.created_at) AS created_date
FROM workflows w1
INNER JOIN workflows w2
    ON DATE(w1.created_at) = DATE(w2.created_at)
    AND w1.id < w2.id  -- avoid duplicates and self-join
ORDER BY created_date, w1.name
LIMIT 20;

-- Aggregate JOIN: workflow name + step count
SELECT w.id, w.name, w.status, COUNT(ws.id) AS step_count
FROM workflows w
LEFT JOIN workflow_steps ws ON ws.workflow_id = w.id
GROUP BY w.id, w.name, w.status
ORDER BY step_count DESC;

-- EXPLAIN ANALYZE on joins — observe join strategy
-- Possible strategies: Nested Loop, Hash Join, Merge Join
-- PostgreSQL chooses based on table size and available indexes.
EXPLAIN ANALYZE
SELECT w.name, ws.name AS step_name
FROM workflows w
INNER JOIN workflow_steps ws ON ws.workflow_id = w.id
WHERE w.status = 'ACTIVE';

EXPLAIN ANALYZE
SELECT w.name, COUNT(ws.id) AS step_count
FROM workflows w
LEFT JOIN workflow_steps ws ON ws.workflow_id = w.id
GROUP BY w.id, w.name;


-- ─────────────────────────────────────────────────────────────────────
-- SCENARIO 18.5: Locking Observation
-- ─────────────────────────────────────────────────────────────────────
--
-- These must be run in TWO SEPARATE psql sessions to observe blocking.
--
-- ── Experiment 1: Row-level lock (FOR UPDATE) ──
--
-- SESSION 1:
--   BEGIN;
--   SELECT * FROM workflows WHERE name = 'Workflow-1' FOR UPDATE;
--   -- Hold the lock open...
--
-- SESSION 2 (in another terminal):
--   BEGIN;
--   UPDATE workflows SET description = 'Blocked update' WHERE name = 'Workflow-1';
--   -- ↑ This BLOCKS. Session 2 waits for Session 1 to release the lock.
--
-- SESSION 1:
--   COMMIT;
--   -- Session 2 unblocks and its UPDATE proceeds.
--
-- SESSION 2:
--   COMMIT;
--
--
-- ── Experiment 2: NOWAIT — fail immediately instead of waiting ──
--
-- SESSION 1:
--   BEGIN;
--   SELECT * FROM workflows WHERE name = 'Workflow-1' FOR UPDATE;
--
-- SESSION 2:
--   BEGIN;
--   SELECT * FROM workflows WHERE name = 'Workflow-1' FOR UPDATE NOWAIT;
--   -- ↑ ERROR: could not obtain lock on row in relation "workflows"
--   -- (fails immediately instead of waiting)
--   ROLLBACK;
--
-- SESSION 1:
--   COMMIT;
--
--
-- ── Experiment 3: SKIP LOCKED — process only unlocked rows ──
--
-- SESSION 1:
--   BEGIN;
--   SELECT * FROM workflows WHERE status = 'DRAFT' ORDER BY created_at LIMIT 5 FOR UPDATE SKIP LOCKED;
--   -- Locks 5 DRAFT workflows
--
-- SESSION 2:
--   BEGIN;
--   SELECT * FROM workflows WHERE status = 'DRAFT' ORDER BY created_at LIMIT 5 FOR UPDATE SKIP LOCKED;
--   -- Returns the NEXT 5 unlocked DRAFT workflows (skips Session 1's locked rows)
--   -- This is the "job queue" pattern!
--
-- Both sessions:
--   COMMIT;
--
--
-- ── Experiment 4: Deadlock ──
--
-- First, find two workflow IDs:
--   SELECT id, name FROM workflows LIMIT 2;
--   -- Let's say: id_A and id_B
--
-- SESSION 1:
--   BEGIN;
--   UPDATE workflows SET description = 'S1' WHERE id = 'id_A';
--
-- SESSION 2:
--   BEGIN;
--   UPDATE workflows SET description = 'S2' WHERE id = 'id_B';
--
-- SESSION 1:
--   UPDATE workflows SET description = 'S1' WHERE id = 'id_B';
--   -- ↑ Waits (Session 2 holds lock on id_B)
--
-- SESSION 2:
--   UPDATE workflows SET description = 'S2' WHERE id = 'id_A';
--   -- ↑ DEADLOCK DETECTED! PostgreSQL kills one transaction:
--   -- ERROR: deadlock detected
--   -- DETAIL: Process X waits for ShareLock on transaction Y; blocked by process Z.
--   -- Process Z waits for ShareLock on transaction X; blocked by process W.
--
-- The killed session must ROLLBACK. The surviving session can COMMIT.
--
--
-- ── Experiment 5: View current locks ──
--
-- Run this while locks are held to see them:
SELECT
    l.locktype,
    l.relation::regclass AS table_name,
    l.mode,
    l.granted,
    a.pid,
    a.usename,
    a.query,
    a.state
FROM pg_locks l
JOIN pg_stat_activity a ON a.pid = l.pid
WHERE l.relation IS NOT NULL
ORDER BY l.relation, l.mode;
