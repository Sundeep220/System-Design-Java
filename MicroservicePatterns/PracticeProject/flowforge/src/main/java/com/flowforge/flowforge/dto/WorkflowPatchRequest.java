package com.flowforge.flowforge.dto;

import com.flowforge.flowforge.validation.ValidWorkflowName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Optional;

// Optional fields: empty Optional = field not provided, present Optional = update the field
// Jackson deserializes missing JSON keys as Optional.empty() for records
public record WorkflowPatchRequest(

        Optional<@Size(max = 100, message = "Name must be at most 100 characters")
                 @ValidWorkflowName String> name,

        Optional<@Size(max = 500, message = "Description must be at most 500 characters") String> description,

        Optional<@Min(value = 0, message = "maxRetries must be >= 0")
                 @Max(value = 10, message = "maxRetries must be <= 10") Integer> maxRetries,

        Optional<@Min(value = 1, message = "timeoutSeconds must be >= 1") Integer> timeoutSeconds
) {
}
