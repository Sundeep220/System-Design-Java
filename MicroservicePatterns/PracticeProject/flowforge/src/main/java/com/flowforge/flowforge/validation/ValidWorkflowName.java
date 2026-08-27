package com.flowforge.flowforge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = WorkflowNameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWorkflowName {

    String message() default "Workflow name must contain only letters, numbers, spaces, hyphens, and underscores";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
