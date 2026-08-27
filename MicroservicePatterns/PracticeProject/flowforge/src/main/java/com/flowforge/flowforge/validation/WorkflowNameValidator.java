package com.flowforge.flowforge.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WorkflowNameValidator implements ConstraintValidator<ValidWorkflowName, String> {

    private static final String VALID_NAME_PATTERN = "^[a-zA-Z0-9 _-]+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // let @NotBlank handle null/blank — don't double-report
            return true;
        }
        return value.matches(VALID_NAME_PATTERN);
    }
}
