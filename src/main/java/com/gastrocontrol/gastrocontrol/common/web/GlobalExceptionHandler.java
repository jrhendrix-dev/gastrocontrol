package com.gastrocontrol.gastrocontrol.common.web;

import com.gastrocontrol.gastrocontrol.common.exception.ApiException;
import com.gastrocontrol.gastrocontrol.common.exception.BusinessRuleViolationException;
import com.gastrocontrol.gastrocontrol.common.exception.EmailSendException;
import com.gastrocontrol.gastrocontrol.common.exception.ValidationException;
import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>(ex.getDetails());
        details.putIfAbsent("message", ex.getMessage());
        details.putIfAbsent("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(ErrorCode.BUSINESS_RULE_VIOLATION, details));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainValidation(ValidationException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>(ex.getDetails());
        details.putIfAbsent("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestValidation(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> details.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> details.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));

        details.putIfAbsent("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.putAll(ex.getDetails());
        details.putIfAbsent("message", ex.getMessage());
        details.putIfAbsent("path", request.getRequestURI());

        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorResponse.of(ex.getCode(), details));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", "Authentication required");
        details.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(ErrorCode.UNAUTHORIZED, details));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", "Access denied");
        details.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(ErrorCode.FORBIDDEN, details));
    }

    /**
     * Email provider failures (Mailgun, SMTP, etc.)
     * - 502 is a good default: “Bad Gateway / upstream failed”
     */
    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailSend(EmailSendException ex, HttpServletRequest request) {
        log.warn("Email send failed on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", "Failed to send email");
        details.put("path", request.getRequestURI());

        // Optional: include ex.getMessage() in dev only (to avoid leaking provider info in prod)
        // details.put("providerMessage", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, details));
        // If you add a dedicated enum value, use it instead:
        // .body(ApiErrorResponse.of(ErrorCode.EMAIL_SEND_FAILED, details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);

        Map<String, String> details = new LinkedHashMap<>();
        details.put("message", "Unexpected error");
        details.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, details));
    }
}
