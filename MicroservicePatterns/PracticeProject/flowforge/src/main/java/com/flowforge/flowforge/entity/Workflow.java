package com.flowforge.flowforge.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflows")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires no-arg constructor
@ToString(exclude = "steps")
public class Workflow extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<WorkflowStep> steps = new ArrayList<>();

    // Flexible Constructor Bodies (Java 25, JEP 513)
    // Validate arguments BEFORE the super() call -- impossible before Java 22+
    public Workflow(String name, String description, int maxRetries, int timeoutSeconds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workflow name must not be blank");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0");
        }
        super();
        this.name = name;
        this.description = description;
        this.maxRetries = maxRetries;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void addStep(WorkflowStep step) {
        steps.add(step);
        step.setWorkflow(this);
    }

    public void removeStep(WorkflowStep step) {
        steps.remove(step);
        step.setWorkflow(null);
    }
}
