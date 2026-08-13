package io.github.terrence721.saga.user.infra.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.github.terrence721.saga.user.exception.InvalidCredentialsException;
import io.github.terrence721.saga.user.exception.UserInactiveException;
import io.github.terrence721.saga.user.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class GrpcExecutor {

    private static final String GENERIC_AUTHENTICATION_FAILURE_MESSAGE = "Invalid email or password";

    public static <T> void execute(StreamObserver<T> responseObserver, Supplier<T> action) {
        try {
            T response = action.get();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (UserNotFoundException | InvalidCredentialsException | UserInactiveException ex) {
            // Deliberately collapsed to one status and one generic message: distinguishing
            // "unknown email" / "wrong password" / "inactive account" at the wire level lets
            // a caller enumerate registered emails (and their active status) without ever
            // guessing a password - CWE-203. The specific reason is still logged server-side.
            log.warn("Authentication failed: {}", ex.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription(GENERIC_AUTHENTICATION_FAILURE_MESSAGE).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid request payload: {}", ex.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.error("Unhandled exception in gRPC service", ex);
            responseObserver.onError(Status.INTERNAL.withDescription("An unexpected internal service error occurred.").asRuntimeException());
        }
    }
}
