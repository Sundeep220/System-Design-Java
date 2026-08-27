package com.flowforge.flowforge.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends FlowForgeException {

    public InvalidStateTransitionException(String currentState, String targetState) {
        super("Cannot transition from %s to %s".formatted(currentState, targetState),
                "INVALID_STATE_TRANSITION", HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
