package com.split.splitwise.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ERROR_CODE);
    }
}
