package io.github.terrence721.saga.user.infra.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class GrpcExecutorTest {

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
