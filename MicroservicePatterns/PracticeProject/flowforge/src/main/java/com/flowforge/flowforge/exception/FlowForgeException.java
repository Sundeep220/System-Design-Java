package com.flowforge.flowforge.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class FlowForgeException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected FlowForgeException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
