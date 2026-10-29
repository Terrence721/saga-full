package com.example.saga.user.exception;

public class UserException extends RuntimeException {
    private final UserErrorCode errorCode;

    public UserException(String message, UserErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserErrorCode errorCode() {
        return errorCode;
    }
}