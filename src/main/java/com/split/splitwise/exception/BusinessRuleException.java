package com.split.splitwise.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends BaseException {

    private static final String ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleException(String message) {
        super(message, HttpStatus.CONFLICT, ERROR_CODE);
    }

    public BusinessRuleException(String message, HttpStatus status) {
        super(message, status, ERROR_CODE);
    }
}
