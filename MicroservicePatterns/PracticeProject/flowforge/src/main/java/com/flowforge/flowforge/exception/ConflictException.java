package com.flowforge.flowforge.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends FlowForgeException {

    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }
}
