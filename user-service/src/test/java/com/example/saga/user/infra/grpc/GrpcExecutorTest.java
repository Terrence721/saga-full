package com.example.saga.user.infra.grpc;

import com.example.saga.user.exception.InvalidCredentialsException;
import com.example.saga.user.exception.UserInactiveException;
import com.example.saga.user.exception.UserNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrpcExecutorTest {

    @Test
    void execute_returnsActionValueAndCompletes() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> "ok");

        assertThat(responseObserver.firstValue()).isEqualTo("ok");
        assertThat(responseObserver.error()).isNull();
    }

    @Test
    void execute_mapsUserNotFoundException_toUnauthenticated() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> {
            throw new UserNotFoundException("no user");
        });

        assertThat(responseObserver.error()).isInstanceOf(StatusRuntimeException.class);
        StatusRuntimeException ex = (StatusRuntimeException) responseObserver.error();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
        assertThat(ex.getStatus().getDescription()).isEqualTo("Invalid email or password");
    }

    @Test
    void execute_mapsInvalidCredentialsException_toUnauthenticated() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> {
            throw new InvalidCredentialsException("bad pw");
        });

        StatusRuntimeException ex = (StatusRuntimeException) responseObserver.error();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
        assertThat(ex.getStatus().getDescription()).isEqualTo("Invalid email or password");
    }

    @Test
    void execute_mapsUserInactiveException_toUnauthenticated() {
        RecordingStreamObserver<String> responseObserver = new RecordingStreamObserver<>();

        GrpcExecutor.execute(responseObserver, () -> {
            throw new UserInactiveException("inactive");
        });

        StatusRuntimeException ex = (StatusRuntimeException) responseObserver.error();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.UNAUTHENTICATED.getCode());
        assertThat(ex.getStatus().getDescription()).isEqualTo("Invalid email or password");
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

    /**
     * The other 3 branches (UserNotFoundException/InvalidCredentialsException/
     * UserInactiveException -> UNAUTHENTICATED, IllegalArgumentException ->
     * INVALID_ARGUMENT) are already exercised transitively through
     * UserGrpcServiceImplErrorTest's real login()/validateToken() calls. This
     * catch-all is the one branch nothing reaches - the real safety net for a
     * genuinely unexpected error, not one of this module's known exception types.
     */
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
