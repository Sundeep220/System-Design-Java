package com.flowforge.flowforge.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends FlowForgeException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super("%s not found with id: %s".formatted(resourceName, id),
                "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
