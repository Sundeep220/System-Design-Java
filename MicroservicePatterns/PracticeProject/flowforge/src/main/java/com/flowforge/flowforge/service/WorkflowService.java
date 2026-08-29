package com.flowforge.flowforge.service;

import com.flowforge.flowforge.dto.CursorPageResponse;
import com.flowforge.flowforge.dto.WorkflowCreateRequest;
import com.flowforge.flowforge.dto.WorkflowFilterRequest;
import com.flowforge.flowforge.dto.WorkflowPatchRequest;
import com.flowforge.flowforge.dto.WorkflowSummaryResponse;
import com.flowforge.flowforge.dto.WorkflowUpdateRequest;
import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.exception.ResourceNotFoundException;
import com.flowforge.flowforge.mapper.WorkflowMapper;
import com.flowforge.flowforge.repository.WorkflowRepository;
import com.flowforge.flowforge.specification.SortValidator;
import com.flowforge.flowforge.specification.WorkflowSpecification;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional(readOnly = true)
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final SortValidator sortValidator;
    private final boolean fullTextEnabled;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowMapper workflowMapper,
                           SortValidator sortValidator,
                           Environment environment) {
        this.workflowRepository = workflowRepository;
        this.workflowMapper = workflowMapper;
        this.sortValidator = sortValidator;
        this.fullTextEnabled = Arrays.asList(environment.getActiveProfiles()).contains("postgres");
    }

    @Transactional
    public Workflow create(WorkflowCreateRequest request) {
        Workflow workflow = workflowMapper.toEntity(request);
        return workflowRepository.save(workflow);
    }

    public Page<Workflow> findAll(WorkflowFilterRequest filter, Pageable pageable) {
        sortValidator.validate(pageable.getSort());

        // PostgreSQL full-text search: uses tsvector/tsquery with GIN index and relevance ranking
        if (fullTextEnabled && filter.search() != null && !filter.search().isBlank()) {
            String statusStr = filter.status() != null ? filter.status().name() : null;
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return workflowRepository.fullTextSearch(
                    filter.search(), statusStr,
                    filter.minRetries(), filter.maxRetries(),
                    filter.createdAfter(), filter.createdBefore(),
                    unsorted);
        }

        // H2 fallback: LIKE-based search via JPA Specifications
        Specification<Workflow> spec = (root, query, cb) -> null;

        if (filter.search() != null && !filter.search().isBlank()) {
            spec = spec.and(WorkflowSpecification.search(filter.search()));
        }
        if (filter.status() != null) {
            spec = spec.and(WorkflowSpecification.hasStatus(filter.status()));
        }
        if (filter.minRetries() != null) {
            spec = spec.and(WorkflowSpecification.minRetries(filter.minRetries()));
        }
        if (filter.maxRetries() != null) {
            spec = spec.and(WorkflowSpecification.maxRetries(filter.maxRetries()));
        }
        if (filter.createdAfter() != null) {
            spec = spec.and(WorkflowSpecification.createdAfter(filter.createdAfter()));
        }
        if (filter.createdBefore() != null) {
            spec = spec.and(WorkflowSpecification.createdBefore(filter.createdBefore()));
        }

        return workflowRepository.findAll(spec, pageable);
    }

    public Workflow findById(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", id));
    }

    public Workflow findByIdWithSteps(UUID id) {
        return workflowRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", id));
    }

    @Transactional
    public Workflow update(UUID id, WorkflowUpdateRequest request) {
        Workflow workflow = findByIdWithSteps(id);
        workflowMapper.updateEntity(workflow, request);
        return workflowRepository.save(workflow);
    }

    @Transactional
    public Workflow patch(UUID id, WorkflowPatchRequest request) {
        Workflow workflow = findByIdWithSteps(id);
        workflowMapper.patchEntity(workflow, request);
        return workflowRepository.save(workflow);
    }

    @Transactional
    public void delete(UUID id) {
        Workflow workflow = findById(id);
        workflowRepository.delete(workflow);
    }

    // --- Cursor-based pagination ---

    public CursorPageResponse<WorkflowSummaryResponse> findAllWithCursor(String cursor, int limit) {
        int fetchSize = Math.min(Math.max(limit, 1), 100);
        // Fetch one extra to determine if there's a next page
        Pageable pageable = PageRequest.of(0, fetchSize + 1);

        List<Workflow> workflows;
        if (cursor == null || cursor.isBlank()) {
            workflows = workflowRepository.findFirstPage(pageable);
        } else {
            CursorData cursorData = decodeCursor(cursor);
            workflows = workflowRepository.findAfterCursor(cursorData.createdAt(), cursorData.id(), pageable);
        }

        boolean hasNext = workflows.size() > fetchSize;
        List<Workflow> page = hasNext ? workflows.subList(0, fetchSize) : workflows;

        List<WorkflowSummaryResponse> content = page.stream()
                .map(workflowMapper::toSummary)
                .toList();

        String nextCursor = hasNext ? encodeCursor(page.getLast()) : null;

        return new CursorPageResponse<>(content, content.size(), hasNext, nextCursor);
    }

    private String encodeCursor(Workflow workflow) {
        String raw = workflow.getCreatedAt().toString() + "|" + workflow.getId().toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorData decodeCursor(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            return new CursorData(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor: " + cursor);
        }
    }

    private record CursorData(Instant createdAt, UUID id) {}
}
