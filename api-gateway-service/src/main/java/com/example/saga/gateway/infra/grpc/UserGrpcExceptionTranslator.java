package com.example.saga.gateway.infra.grpc;

import org.springframework.stereotype.Component;

import com.example.saga.gateway.exception.DependencyUnavailableException;
import com.example.saga.gateway.exception.InvalidCredentialsException;
import com.example.saga.gateway.exception.UserInactiveException;
import com.example.saga.gateway.exception.UserNotFoundException;
import io.grpc.StatusRuntimeException;

@Component
public class UserGrpcExceptionTranslator {

    public RuntimeException translate(StatusRuntimeException ex) {

        return switch (ex.getStatus().getCode()) {

            case UNAUTHENTICATED ->
                new InvalidCredentialsException("Invalid email or password provided");

            case NOT_FOUND ->
                new UserNotFoundException("User account does not exist");

            case INVALID_ARGUMENT ->
                new IllegalArgumentException("Invalid user request fields");

            case PERMISSION_DENIED ->
                new UserInactiveException("User account is currently deactivated");

            case UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL ->
                new DependencyUnavailableException("User authentication service is temporarily unavailable", ex);

            default ->
                new DependencyUnavailableException("Unexpected internal gRPC system error occurred", ex);
        };
    }
}
