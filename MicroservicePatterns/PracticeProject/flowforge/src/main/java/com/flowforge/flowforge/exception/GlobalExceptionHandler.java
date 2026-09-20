package com.flowforge.flowforge.exception;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- FlowForge custom exceptions (ResourceNotFound, Conflict, InvalidStateTransition) ---
    @ExceptionHandler(FlowForgeException.class)
    public ResponseEntity<ApiError> handleFlowForgeException(FlowForgeException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                ex.getHttpStatus().value(),
                ex.getHttpStatus().getReasonPhrase(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    // --- Bean Validation failures (@Valid) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getRejectedValue()
                ))
                .toList();

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_FAILED",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }

    // --- Method-level @Validated violations (service layer) ---
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> new ApiError.FieldError(
                        cv.getPropertyPath().toString(),
                        cv.getMessage(),
                        cv.getInvalidValue()
                ))
                .toList();

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_FAILED",
                "Constraint violation",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }

    // --- Malformed JSON body ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "MALFORMED_REQUEST",
                "Request body is missing or malformed",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.badRequest().body(error);
    }

    // --- Invalid arguments (e.g., bad cursor) ---
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "INVALID_PARAMETER",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.badRequest().body(error);
    }

    // --- Database constraint violations (e.g., unique index, CHECK, FK) ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "A data conflict occurred. Check for duplicate or invalid references.";
        String errorCode = "DATA_INTEGRITY_VIOLATION";

        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage != null) {
            if (rootMessage.contains("uq_workflow_name")) {
                message = "A workflow with this name already exists.";
                errorCode = "DUPLICATE_NAME";
            } else if (rootMessage.contains("chk_workflow_status")) {
                message = "Invalid workflow status. Allowed: DRAFT, ACTIVE, PAUSED, ARCHIVED.";
                errorCode = "INVALID_STATUS";
            } else if (rootMessage.contains("chk_workflow_max_retries")) {
                message = "max_retries must be >= 0.";
                errorCode = "INVALID_MAX_RETRIES";
            } else if (rootMessage.contains("chk_workflow_timeout")) {
                message = "timeout_seconds must be > 0.";
                errorCode = "INVALID_TIMEOUT";
            } else if (rootMessage.contains("chk_step_order")) {
                message = "step_order must be >= 0.";
                errorCode = "INVALID_STEP_ORDER";
            }
        }

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                errorCode,
                message,
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // --- Optimistic locking conflict (Step 22) ---
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "OPTIMISTIC_LOCK_CONFLICT",
                "Entity was modified by another user. Please retry.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // --- Transaction timeout (Step 21) ---
    @ExceptionHandler(TransactionTimedOutException.class)
    public ResponseEntity<ApiError> handleTransactionTimeout(TransactionTimedOutException ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.GATEWAY_TIMEOUT.value(),
                HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase(),
                "TRANSACTION_TIMEOUT",
                "Operation timed out",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error);
    }

    // --- Catch-all: no stack trace to the client ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
