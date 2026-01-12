package com.gastrocontrol.gastrocontrol.common.web;

import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;

import java.util.Map;

/**
 * Standard API error payload.
 *
 * Example:
 * {
 *   "error": {
 *     "code": "VALIDATION_FAILED",
 *     "details": {
 *       "email": "email not valid because..."
 *     }
 *   }
 * }
 */
public record ApiErrorResponse(ErrorBody error) {

    /**
     * Convenience factory.
     *
     * @param code    high-level error code
     * @param details field/message map
     * @return response instance
     */
    public static ApiErrorResponse of(ErrorCode code, Map<String, String> details) {
        return new ApiErrorResponse(new ErrorBody(code, details));
    }

    /**
     * Nested error body for consistent JSON structure.
     *
     * @param code    error code
     * @param details details map
     */
    public record ErrorBody(ErrorCode code, Map<String, String> details) { }
}
