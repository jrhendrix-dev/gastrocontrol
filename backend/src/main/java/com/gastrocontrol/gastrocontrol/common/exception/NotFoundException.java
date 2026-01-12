package com.gastrocontrol.gastrocontrol.common.exception;

import com.gastrocontrol.gastrocontrol.common.exception.enums.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND);
    }
}
