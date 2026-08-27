package com.flowforge.flowforge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "workflow_steps")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires no-arg constructor
@ToString(exclude = "workflow")
public class WorkflowStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(columnDefinition = "TEXT")
    private String config;

    // Flexible Constructor Bodies (Java 25, JEP 513)
    // Validate arguments BEFORE the super() call -- impossible before Java 22+
    public WorkflowStep(Workflow workflow, String name, String type, int stepOrder, String config) {
        if (workflow == null) {
            throw new IllegalArgumentException("WorkflowStep must belong to a Workflow");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Step name must not be blank");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Step type must not be blank");
        }
        if (stepOrder < 0) {
            throw new IllegalArgumentException("stepOrder must be >= 0");
        }
        super();
        this.workflow = workflow;
        this.name = name;
        this.type = type;
        this.stepOrder = stepOrder;
        this.config = config;
    }
}
