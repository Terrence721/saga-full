package com.example.saga.user.exception;

public class InvalidCredentialsException extends UserException {
    public InvalidCredentialsException(String message) {
        super(message, UserErrorCode.INVALID_CREDENTIALS);
    }
}