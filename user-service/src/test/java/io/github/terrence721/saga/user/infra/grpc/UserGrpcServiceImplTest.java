package io.github.terrence721.saga.user.infra.grpc;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.terrence721.saga.user.domain.User;
import io.github.terrence721.saga.user.grpc.LoginRequest;
import io.github.terrence721.saga.user.grpc.LoginResponse;
import io.github.terrence721.saga.user.grpc.ValidateTokenRequest;
import io.github.terrence721.saga.user.grpc.ValidateTokenResponse;
import io.github.terrence721.saga.user.infra.security.JwtTokenProvider;
import io.github.terrence721.saga.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class UserGrpcServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserGrpcServiceImpl service;

    private User activeUser(UUID id) {
        User user = User.builder()
                .id(id)
                .email("user@example.com")
                .passwordHash("hashed-password")
                .active(true)
                .build();
        return user;
    }

    @Test
    void loginSucceedsAndReturnsAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = activeUser(userId);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.createToken(user)).thenReturn(new JwtTokenProvider.IssuedToken("signed-token", 3600L));

        LoginRequest request = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("correct-password")
                .build();

        RecordingStreamObserver<LoginResponse> observer = new RecordingStreamObserver<>();
        service.login(request, observer);

        assertThat(observer.error()).isNull();
        LoginResponse response = observer.firstValue();
        assertThat(response.getUserId()).isEqualTo(userId.toString());
        assertThat(response.getAccessToken()).isEqualTo("signed-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void validateTokenReturnsValidWhenTokenVerifies() {
        UUID userId = UUID.randomUUID();

        Claim userIdClaim = mockClaim(userId.toString());
        DecodedJWT decodedJWT = Mockito.mock(DecodedJWT.class);
        when(decodedJWT.getClaim("user-id")).thenReturn(userIdClaim);
        when(jwtTokenProvider.verifyToken("valid-token")).thenReturn(Optional.of(decodedJWT));

        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setAccessToken("valid-token")
                .build();

        RecordingStreamObserver<ValidateTokenResponse> observer = new RecordingStreamObserver<>();
        service.validateToken(request, observer);

        assertThat(observer.error()).isNull();
        ValidateTokenResponse response = observer.firstValue();
        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(userId.toString());
    }

    private Claim mockClaim(String value) {
        Claim claim = Mockito.mock(Claim.class);
        when(claim.asString()).thenReturn(value);
        return claim;
    }
}
