package com.gastrocontrol.gastrocontrol.common.exception;

import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;
    private final Map<String, String> details;

    public ApiException(String message, HttpStatus status, ErrorCode code) {
        this(message, status, code, null);
    }

    public ApiException(String message, HttpStatus status, ErrorCode code, Map<String, String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public HttpStatus getStatus() { return status; }
    public ErrorCode getCode() { return code; }
    public Map<String, String> getDetails() { return details; }
}
