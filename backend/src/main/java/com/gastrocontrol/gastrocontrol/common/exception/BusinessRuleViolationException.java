package com.gastrocontrol.gastrocontrol.common.exception;

import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    public BusinessRuleViolationException(Map<String, String> details) {
        super(
                details != null && details.containsKey("message") ? details.get("message") : "Business rule violated",
                HttpStatus.CONFLICT,
                ErrorCode.BUSINESS_RULE_VIOLATION,
                details
        );
    }

    public BusinessRuleViolationException(String message, Map<String, String> details) {
        super(message, HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, details);
    }
}
