package com.flowforge.flowforge.dto;

import com.flowforge.flowforge.validation.group.OnUpdate;
import com.flowforge.flowforge.validation.ValidWorkflowName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkflowUpdateRequest(

        @NotBlank(message = "Name is required", groups = OnUpdate.class)
        @Size(max = 100, message = "Name must be at most 100 characters")
        @ValidWorkflowName
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Min(value = 0, message = "maxRetries must be >= 0")
        @Max(value = 10, message = "maxRetries must be <= 10")
        int maxRetries,

        @Min(value = 1, message = "timeoutSeconds must be >= 1")
        int timeoutSeconds
) {
}
