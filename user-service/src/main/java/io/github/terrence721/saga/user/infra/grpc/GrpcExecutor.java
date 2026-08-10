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

    public static <T> void execute(StreamObserver<T> responseObserver, Supplier<T> action) {
        try {
            T response = action.get();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (UserNotFoundException ex) {
            log.warn("User not found: {}", ex.getMessage());
            responseObserver.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (InvalidCredentialsException ex) {
            log.warn("Invalid credentials: {}", ex.getMessage());
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription(ex.getMessage()).asRuntimeException());
        } catch (UserInactiveException ex) {
            log.warn("User inactive: {}", ex.getMessage());
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(ex.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid request payload: {}", ex.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.error("Unhandled exception in gRPC service", ex);
            responseObserver.onError(Status.INTERNAL.withDescription("An unexpected internal service error occurred.").asRuntimeException());
        }
    }
}
