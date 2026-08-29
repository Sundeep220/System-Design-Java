package com.flowforge.flowforge.exception;

import org.springframework.http.HttpStatus;

import java.util.Set;

public class InvalidSortFieldException extends FlowForgeException {

    public InvalidSortFieldException(String field, Set<String> allowed) {
        super(
                "Invalid sort field: '" + field + "'. Allowed fields: " + allowed,
                "INVALID_SORT_FIELD",
                HttpStatus.BAD_REQUEST
        );
    }
}
