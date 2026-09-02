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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
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
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("hashed-password")
                .active(active)
                .build();
        return user;
    }

    private LoginRequest loginRequest(String password) {
        return LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword(password)
                .build();
    }

    /**
     * Unknown email, wrong password, and an inactive account all collapse to the same
     * generic UNAUTHENTICATED status/message - distinguishing them at the wire level would
     * let a caller enumerate registered emails (and their active status) without ever
     * guessing a password (CWE-203). See GrpcExecutor.
     */
    @Test
    void loginFailsWithGenericUnauthenticatedWhenUserMissing() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("any-password"), observer);

        assertThat(observer.firstValueOrNull()).isNull();
        Status status = Status.fromThrowable(observer.error());
        assertThat(status.getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(status.getDescription()).isEqualTo("Invalid email or password");
    }

    @Test
    void loginFailsWithGenericUnauthenticatedWhenUserInactive() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(userWithActive(false)));

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("any-password"), observer);

        assertThat(observer.firstValueOrNull()).isNull();
        Status status = Status.fromThrowable(observer.error());
        assertThat(status.getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(status.getDescription()).isEqualTo("Invalid email or password");
    }

    @Test
    void loginFailsWithGenericUnauthenticatedWhenPasswordWrong() {
        User user = userWithActive(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("wrong-password"), observer);

        assertThat(observer.firstValueOrNull()).isNull();
        Status status = Status.fromThrowable(observer.error());
        assertThat(status.getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(status.getDescription()).isEqualTo("Invalid email or password");
    }

    /**
     * BCrypt verification is deliberately slow; skipping it whenever no user is found
     * would let an attacker distinguish "unknown email" from "wrong password" purely by
     * response latency, even though both now return the identical UNAUTHENTICATED status
     * (see GrpcExecutor). Proves the constant-cost comparison is actually wired in, not
     * just that the exception/status still comes out right.
     */
    @Test
    void loginComparesAgainstADummyHashWhenUserNotFound_forTimingSafety() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("any-password"), observer);

        verify(passwordEncoder, times(1)).matches(eq("any-password"), anyString());
    }

    /**
     * Same timing-safety guarantee for an inactive account: the original code threw
     * before ever calling passwordEncoder.matches() here, giving it a smaller timing
     * footprint than a real login attempt against an active account.
     */
    @Test
    void loginStillComparesPasswordWhenUserInactive_forTimingSafety() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(userWithActive(false)));

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(loginRequest("any-password"), observer);

        verify(passwordEncoder, times(1)).matches(eq("any-password"), eq("hashed-password"));
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
