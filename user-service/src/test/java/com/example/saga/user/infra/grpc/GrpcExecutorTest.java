package com.example.saga.user.infra.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcExecutorTest {

    @Test
    void constructor_canBeInstantiated() {
        GrpcExecutor executor = new GrpcExecutor();
        org.assertj.core.api.Assertions.assertThat(executor).isNotNull();
    }

    @Test
    void execute_returnsActionValueAndCompletes() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> "ok");

        assertThat(responseObserver.firstValue()).isEqualTo("ok");
        assertThat(responseObserver.error()).isNull();
    }

    @Test
    void execute_mapsIllegalArgumentException_toInvalidArgument() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> {
            throw new IllegalArgumentException("bad payload");
        });

        StatusRuntimeException ex = (StatusRuntimeException) responseObserver.error();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
        assertThat(ex.getStatus().getDescription()).isEqualTo("bad payload");
    }

    @Test
    void execute_MapsAnUnexpectedException_ToInternalStatus() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> {
            throw new RuntimeException("something genuinely unexpected");
        });

        assertThat(responseObserver.firstValueOrNull()).isNull();
        assertThat(responseObserver.error()).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException statusException = (StatusRuntimeException) responseObserver.error();
        assertThat(statusException.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
        assertThat(statusException.getStatus().getDescription()).isEqualTo("An unexpected internal service error occurred.");
    }
}