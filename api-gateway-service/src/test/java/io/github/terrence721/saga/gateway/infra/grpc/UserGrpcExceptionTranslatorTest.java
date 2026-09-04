package io.github.terrence721.saga.gateway.infra.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.terrence721.saga.gateway.exception.DependencyUnavailableException;
import io.github.terrence721.saga.gateway.exception.InvalidCredentialsException;
import io.github.terrence721.saga.gateway.exception.UserInactiveException;
import io.github.terrence721.saga.gateway.exception.UserNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class UserGrpcExceptionTranslatorTest {

    private final UserGrpcExceptionTranslator translator = new UserGrpcExceptionTranslator();

    @Test
    void translate_ReturnsInvalidCredentialsException_ForUnauthenticated() {
        StatusRuntimeException grpcError = Status.UNAUTHENTICATED.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(InvalidCredentialsException.class);
        assertThat(translated.getMessage()).isEqualTo("Invalid email or password provided");
    }

    @Test
    void translate_ReturnsUserNotFoundException_ForNotFound() {
        StatusRuntimeException grpcError = Status.NOT_FOUND.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(UserNotFoundException.class);
        assertThat(translated.getMessage()).isEqualTo("User account does not exist");
    }

    @Test
    void translate_ReturnsIllegalArgumentException_ForInvalidArgument() {
        StatusRuntimeException grpcError = Status.INVALID_ARGUMENT.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(IllegalArgumentException.class);
        assertThat(translated.getMessage()).isEqualTo("Invalid user request fields");
    }

    @Test
    void translate_ReturnsUserInactiveException_ForPermissionDenied() {
        StatusRuntimeException grpcError = Status.PERMISSION_DENIED.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(UserInactiveException.class);
        assertThat(translated.getMessage()).isEqualTo("User account is currently deactivated");
    }

    @Test
    void translate_ReturnsDependencyUnavailableExceptionWithCause_ForUnavailable() {
        StatusRuntimeException grpcError = Status.UNAVAILABLE.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(DependencyUnavailableException.class);
        assertThat(translated.getMessage()).isEqualTo("User authentication service is temporarily unavailable");
        assertThat(translated.getCause()).isSameAs(grpcError);
    }

    @Test
    void translate_ReturnsDependencyUnavailableExceptionWithCause_ForDeadlineExceeded() {
        StatusRuntimeException grpcError = Status.DEADLINE_EXCEEDED.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(DependencyUnavailableException.class);
        assertThat(translated.getMessage()).isEqualTo("User authentication service is temporarily unavailable");
        assertThat(translated.getCause()).isSameAs(grpcError);
    }

    @Test
    void translate_ReturnsDependencyUnavailableExceptionWithCause_ForInternal() {
        StatusRuntimeException grpcError = Status.INTERNAL.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(DependencyUnavailableException.class);
        assertThat(translated.getMessage()).isEqualTo("User authentication service is temporarily unavailable");
        assertThat(translated.getCause()).isSameAs(grpcError);
    }

    @Test
    void translate_ReturnsDependencyUnavailableExceptionWithCause_ForAnyOtherStatus() {
        StatusRuntimeException grpcError = Status.RESOURCE_EXHAUSTED.asRuntimeException();

        RuntimeException translated = translator.translate(grpcError);

        assertThat(translated).isInstanceOf(DependencyUnavailableException.class);
        assertThat(translated.getMessage()).isEqualTo("Unexpected internal gRPC system error occurred");
        assertThat(translated.getCause()).isSameAs(grpcError);
    }
}
