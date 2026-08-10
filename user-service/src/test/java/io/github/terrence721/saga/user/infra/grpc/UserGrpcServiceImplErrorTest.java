package io.github.terrence721.saga.user.infra.grpc;

import io.github.terrence721.saga.user.domain.User;
import io.github.terrence721.saga.user.grpc.LoginRequest;
import io.github.terrence721.saga.user.grpc.LoginResponse;
import io.github.terrence721.saga.user.grpc.ValidateTokenRequest;
import io.github.terrence721.saga.user.grpc.ValidateTokenResponse;
import io.github.terrence721.saga.user.infra.security.JwtTokenProvider;
import io.github.terrence721.saga.user.repository.UserRepository;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGrpcServiceImplErrorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserGrpcServiceImpl service;

    private User userWithActive(boolean active) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashed-password")
                .active(active)
                .build();
    }

    private LoginRequest loginRequest(String password) {
        return LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword(password)
                .build();
    }

    @Test
    void loginFailsWithNotFoundWhenUserMissing() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("any-password"), observer);

        assertThat(observer.firstValueOrNull()).isNull();
        assertThat(Status.fromThrowable(observer.error()).getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void loginFailsWithPermissionDeniedWhenUserInactive() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(userWithActive(false)));

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("any-password"), observer);

        assertThat(observer.firstValueOrNull()).isNull();
        assertThat(Status.fromThrowable(observer.error()).getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
    }

    @Test
    void loginFailsWithUnauthenticatedWhenPasswordWrong() {
        User user = userWithActive(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("wrong-password"), observer);

        assertThat(observer.firstValueOrNull()).isNull();
        assertThat(Status.fromThrowable(observer.error()).getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }

    @Test
    void validateTokenReturnsInvalidWhenTokenVerificationFails() {
        when(jwtTokenProvider.verifyToken("bad-token")).thenReturn(Optional.empty());

        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setAccessToken("bad-token")
                .build();

        RecordingStreamObserver<ValidateTokenResponse> observer = new RecordingStreamObserver<>();
        service.validateToken(request, observer);

        assertThat(observer.error()).isNull();
        ValidateTokenResponse response = observer.firstValue();
        assertThat(response.getValid()).isFalse();
        assertThat(response.getUserId()).isEmpty();
    }
}
