package com.flowforge.flowforge.repository;

import com.flowforge.flowforge.entity.Workflow;
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
