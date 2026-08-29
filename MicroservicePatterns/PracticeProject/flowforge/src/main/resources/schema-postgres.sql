-- Full-text search: GIN expression index on workflows
-- This index pre-computes the tsvector for (name + description) and stores it in a GIN structure.
-- PostgreSQL can then use this index for @@ (text search match) queries instead of scanning every row.

CREATE INDEX IF NOT EXISTS idx_workflows_fts
    ON workflows
    USING GIN (to_tsvector('english', name || ' ' || coalesce(description, '')));
