package com.example.saga.user.exception;

public class UserNotFoundException extends UserException {
    public UserNotFoundException(String message) {
        super(message, UserErrorCode.USER_NOT_FOUND);
    }
}