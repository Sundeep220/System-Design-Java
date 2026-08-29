package com.flowforge.flowforge.specification;

import com.flowforge.flowforge.entity.Workflow;
import com.flowforge.flowforge.entity.WorkflowStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class WorkflowSpecification {

    private WorkflowSpecification() {}

    public static Specification<Workflow> hasStatus(WorkflowStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Workflow> minRetries(int minRetries) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxRetries"), minRetries);
    }

    public static Specification<Workflow> maxRetries(int maxRetries) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("maxRetries"), maxRetries);
    }

    public static Specification<Workflow> createdAfter(Instant after) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), after);
    }

    public static Specification<Workflow> createdBefore(Instant before) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), before);
    }

    public static Specification<Workflow> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Workflow> search(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }
}
