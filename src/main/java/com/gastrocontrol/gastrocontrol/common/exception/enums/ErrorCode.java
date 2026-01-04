// src/main/java/com/gastrocontrol/gastrocontrol/common/exception/enums/ErrorCode.java
package com.gastrocontrol.gastrocontrol.common.exception.enums;

/**
 * High-level error codes returned by the API.
 */
public enum ErrorCode {
    VALIDATION_FAILED,
    NOT_FOUND,
    BUSINESS_RULE_VIOLATION,

    UNAUTHORIZED,
    FORBIDDEN,

    INTERNAL_ERROR
}
