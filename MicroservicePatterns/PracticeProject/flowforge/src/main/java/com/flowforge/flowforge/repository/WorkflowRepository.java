package com.flowforge.flowforge.repository;

import com.flowforge.flowforge.dto.WorkflowIdNameStatus;
import com.flowforge.flowforge.dto.WorkflowProjection;
import com.flowforge.flowforge.entity.Workflow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID>,
        JpaSpecificationExecutor<Workflow> {

    @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.steps WHERE w.id = :id")
    Optional<Workflow> findByIdWithSteps(UUID id);

    // PostgreSQL full-text search with relevance ranking
    @Query(value = """
            SELECT w.* FROM workflows w
            WHERE to_tsvector('english', w.name || ' ' || coalesce(w.description, ''))
                  @@ plainto_tsquery('english', :query)
              AND (:status IS NULL OR w.status = :status)
              AND (:minRetries IS NULL OR w.max_retries >= :minRetries)
              AND (:maxRetries IS NULL OR w.max_retries <= :maxRetries)
              AND (CAST(:createdAfter AS TIMESTAMP WITH TIME ZONE) IS NULL OR w.created_at >= CAST(:createdAfter AS TIMESTAMP WITH TIME ZONE))
              AND (CAST(:createdBefore AS TIMESTAMP WITH TIME ZONE) IS NULL OR w.created_at <= CAST(:createdBefore AS TIMESTAMP WITH TIME ZONE))
            ORDER BY ts_rank(
                to_tsvector('english', w.name || ' ' || coalesce(w.description, '')),
                plainto_tsquery('english', :query)
            ) DESC
            """,
            countQuery = """
            SELECT count(*) FROM workflows w
            WHERE to_tsvector('english', w.name || ' ' || coalesce(w.description, ''))
                  @@ plainto_tsquery('english', :query)
              AND (:status IS NULL OR w.status = :status)
              AND (:minRetries IS NULL OR w.max_retries >= :minRetries)
              AND (:maxRetries IS NULL OR w.max_retries <= :maxRetries)
              AND (CAST(:createdAfter AS TIMESTAMP WITH TIME ZONE) IS NULL OR w.created_at >= CAST(:createdAfter AS TIMESTAMP WITH TIME ZONE))
              AND (CAST(:createdBefore AS TIMESTAMP WITH TIME ZONE) IS NULL OR w.created_at <= CAST(:createdBefore AS TIMESTAMP WITH TIME ZONE))
            """,
            nativeQuery = true)
    Page<Workflow> fullTextSearch(
            @Param("query") String query,
            @Param("status") String status,
            @Param("minRetries") Integer minRetries,
            @Param("maxRetries") Integer maxRetries,
            @Param("createdAfter") Instant createdAfter,
            @Param("createdBefore") Instant createdBefore,
            Pageable pageable);

    // ── Step 20: N+1 fixes ──

    // Fix 1: JOIN FETCH — single query, but breaks pagination
    @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.steps")
    List<Workflow> findAllWithSteps();

    // Fix 2: @EntityGraph — LEFT JOIN, works better with derived queries
    @EntityGraph(attributePaths = {"steps"})
    @Query("SELECT w FROM Workflow w")
    List<Workflow> findAllWithStepsEntityGraph();

    // ── Step 20: DTO Projections ──

    // Interface projection — only id, name, status columns selected
    List<WorkflowProjection> findAllProjectedBy();

    // Class-based DTO projection with constructor expression
    @Query("SELECT new com.flowforge.flowforge.dto.WorkflowIdNameStatus(w.id, w.name, w.status) FROM Workflow w")
    List<WorkflowIdNameStatus> findAllDtoProjection();

    // Cursor pagination: fetch first page (no cursor)
    @Query("SELECT w FROM Workflow w ORDER BY w.createdAt DESC, w.id DESC")
    List<Workflow> findFirstPage(org.springframework.data.domain.Pageable pageable);

    // Cursor pagination: fetch next page after cursor
    @Query("""
            SELECT w FROM Workflow w
            WHERE w.createdAt < :cursorTime
               OR (w.createdAt = :cursorTime AND w.id < :cursorId)
            ORDER BY w.createdAt DESC, w.id DESC
            """)
    List<Workflow> findAfterCursor(Instant cursorTime, UUID cursorId,
                                   org.springframework.data.domain.Pageable pageable);
}
