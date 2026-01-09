package com.gastrocontrol.gastrocontrol.common.exception;

import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class EmailSendException extends ApiException {
    public EmailSendException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, ErrorCode.EMAIL_SEND_FAILED);
    }

    public EmailSendException(String message, Map<String, String> details) {
        super(message, HttpStatus.BAD_GATEWAY, ErrorCode.EMAIL_SEND_FAILED, details);
    }
}
