package com.flowforge.flowforge.repository;

import com.flowforge.flowforge.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID>,
        JpaSpecificationExecutor<Workflow> {

    @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.steps WHERE w.id = :id")
    Optional<Workflow> findByIdWithSteps(UUID id);

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
