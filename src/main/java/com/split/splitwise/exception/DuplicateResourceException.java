package com.split.splitwise.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
                String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.CONFLICT,
                ERROR_CODE
        );
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, ERROR_CODE);
    }
}
