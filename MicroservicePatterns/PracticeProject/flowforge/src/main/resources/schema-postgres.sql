-- =====================================================================
-- FlowForge Schema Enhancements — Phase 2, Step 18: Database Fundamentals
-- =====================================================================
-- Hibernate auto-generates tables from @Entity classes, but it does NOT
-- create CHECK constraints, UNIQUE constraints (unless @Column(unique=true)),
-- or performance indexes. We add them here manually.
--
-- This file runs on every startup (spring.sql.init.mode=always) so every
-- statement MUST be idempotent (IF NOT EXISTS / DO $$ blocks).
-- =====================================================================


-- ─────────────────────────────────────────────────────────────────────
-- SCENARIO 18.1: Constraints
-- ─────────────────────────────────────────────────────────────────────

-- CHECK: workflow status must be one of the enum values.
-- Hibernate stores @Enumerated(EnumType.STRING) as VARCHAR — no DB-level validation.
-- This CHECK ensures bad data can never sneak in via native SQL or manual inserts.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_workflow_status'
    ) THEN
        ALTER TABLE workflows
            ADD CONSTRAINT chk_workflow_status
            CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ARCHIVED'));
    END IF;
END $$;

-- CHECK: max_retries must be non-negative
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_workflow_max_retries'
    ) THEN
        ALTER TABLE workflows
            ADD CONSTRAINT chk_workflow_max_retries
            CHECK (max_retries >= 0);
    END IF;
END $$;

-- CHECK: timeout_seconds must be positive
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_workflow_timeout'
    ) THEN
        ALTER TABLE workflows
            ADD CONSTRAINT chk_workflow_timeout
            CHECK (timeout_seconds > 0);
    END IF;
END $$;

-- UNIQUE: no two workflows should have the same name
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_workflow_name'
    ) THEN
        ALTER TABLE workflows
            ADD CONSTRAINT uq_workflow_name UNIQUE (name);
    END IF;
END $$;

-- CHECK: step_order must be non-negative
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_step_order'
    ) THEN
        ALTER TABLE workflow_steps
            ADD CONSTRAINT chk_step_order
            CHECK (step_order >= 0);
    END IF;
END $$;


-- ─────────────────────────────────────────────────────────────────────
-- SCENARIO 18.2: Normalization Audit (documentation)
-- ─────────────────────────────────────────────────────────────────────
--
-- Is our schema in 3NF?  YES.
--
-- workflows table:
--   PK: id (UUID)
--   All non-key columns (name, description, status, max_retries, timeout_seconds,
--   created_at, updated_at) depend on the WHOLE key (id) and NOTHING BUT the key.
--   No transitive dependencies. ✅ 3NF
--
-- workflow_steps table:
--   PK: id (UUID)
--   FK: workflow_id → workflows.id
--   All non-key columns (name, type, step_order, config) depend only on id.
--   workflow_id is a FK (relationship), not a partial key dependency. ✅ 3NF
--
-- No repeating groups (1NF ✅), no partial dependencies (2NF ✅ — single-column PKs),
-- no transitive dependencies (3NF ✅).
--
-- Denormalization consideration:
--   If we frequently need "workflow name + step count" for list views, we COULD
--   add a step_count column to workflows (denormalized). But for now, a JOIN or
--   COUNT subquery is fast enough with proper indexes. No denormalization needed.


-- ─────────────────────────────────────────────────────────────────────
-- SCENARIO 18.3: Index Design
-- ─────────────────────────────────────────────────────────────────────

-- 1. Composite index: (status, name) for filtered listing
--    Supports: WHERE status = ? AND name = ?  (both columns)
--              WHERE status = ?               (leftmost prefix)
--    Does NOT support: WHERE name = ?          (not leftmost — needs separate index or Seq Scan)
CREATE INDEX IF NOT EXISTS idx_workflow_status_name
    ON workflows (status, name);

-- 2. FK index on workflow_steps.workflow_id
--    PostgreSQL does NOT auto-create indexes on FK columns (unlike MySQL).
--    Without this, every JOIN or CASCADE DELETE does a Seq Scan on workflow_steps.
CREATE INDEX IF NOT EXISTS idx_step_workflow_id
    ON workflow_steps (workflow_id);

-- 3. Covering index: status + INCLUDE(name, id)
--    For queries: SELECT id, name FROM workflows WHERE status = ?
--    → Index Only Scan (no heap fetch needed — all columns are in the index)
CREATE INDEX IF NOT EXISTS idx_workflow_status_cover
    ON workflows (status) INCLUDE (name, id);

-- 4. Full-text search: GIN expression index on workflows
--    Pre-computes the tsvector for (name + description) and stores it in a GIN structure.
--    PostgreSQL can then use this index for @@ (text search match) queries.
CREATE INDEX IF NOT EXISTS idx_workflows_fts
    ON workflows
    USING GIN (to_tsvector('english', name || ' ' || coalesce(description, '')));

-- 5. Index on created_at for cursor pagination and date range queries
CREATE INDEX IF NOT EXISTS idx_workflow_created_at
    ON workflows (created_at DESC, id DESC);
