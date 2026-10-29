package com.example.saga.user.exception;

public class UserInactiveException extends UserException {
    public UserInactiveException(String message) {
        super(message, UserErrorCode.USER_INACTIVE);
    }
}