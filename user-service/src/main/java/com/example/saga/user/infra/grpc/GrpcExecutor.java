package com.example.saga.user.infra.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class GrpcExecutor {

    public static <T> void execute(StreamObserver<T> responseObserver, Supplier<T> action) {
        try {
            T response = action.get();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid request payload: {}", ex.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.error("Unhandled exception in gRPC service", ex);
            responseObserver.onError(Status.INTERNAL.withDescription("An unexpected internal service error occurred.").asRuntimeException());
        }
    }
}
