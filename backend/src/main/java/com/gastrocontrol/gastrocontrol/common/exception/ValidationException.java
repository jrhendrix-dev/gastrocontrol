// src/main/java/com/gastrocontrol/gastrocontrol/common/exception/ValidationException.java
package com.gastrocontrol.gastrocontrol.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception representing validation errors with field-specific messages.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> details;

    public ValidationException(Map<String, String> details) {
        super("Validation failed");
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
