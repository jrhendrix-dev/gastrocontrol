package com.gastrocontrol.gastrocontrol.common.web;

import com.gastrocontrol.gastrocontrol.common.exception.ApiException;
import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;
import com.gastrocontrol.gastrocontrol.common.exception.NotFoundException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception-to-HTTP mapping. Guarantees a stable error response shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Custom API exception for business rule violations and controlled errors.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ErrorCode.BUSINESS_RULE_VIOLATION;

        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", ex.getMessage());
        details.put("path", request.getRequestURI());

        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(code, details));
    }

    /**
     * Custom NotFoundException (more specific than ApiException).
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", ex.getMessage());
        details.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(ErrorCode.NOT_FOUND, details));
    }

    /**
     * Domain/application validation errors coming from your use-cases.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_FAILED, ex.getDetails()));
    }

    /**
     * Request DTO validation errors (@Valid + Bean Validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(fe -> {
            // fe.getField() already includes indexes like "items[0].quantity"
            details.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        });

        ex.getBindingResult().getGlobalErrors().forEach(ge -> {
            details.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_FAILED, details));
    }

    /**
     * Catch-all for unexpected exceptions.
     * Don't leak internal exception messages in production.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(
            Exception ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", "Unexpected error");
        details.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(ErrorCode.BUSINESS_RULE_VIOLATION, details));
    }
}
