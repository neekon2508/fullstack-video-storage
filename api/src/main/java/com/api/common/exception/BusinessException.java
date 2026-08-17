package com.api.common.exception;

import java.io.Serial;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String message;
    private final String statusCode;

    public BusinessException(String message, String statuscode) {
        this.message = message;
        this.statusCode = statuscode;
    }

    public BusinessException(String message) {
        this(message, ErrorCode.FAIL.getCode());
    }

    public BusinessException(String message, Throwable cause) {
        this(message, ErrorCode.FAIL.getCode(), cause);
    }

    public BusinessException(String message, String statusCode, Throwable cause) {
        super(cause);
        this.message = message;
        this.statusCode = statusCode;
    }

    public BusinessException(ErrorCode e) {
        this.message = e.getMessage();
        this.statusCode = e.getCode();
    }
}
