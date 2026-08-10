package io.github.terrence721.saga.user.infra.grpc;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.terrence721.saga.user.domain.User;
import io.github.terrence721.saga.user.grpc.LoginRequest;
import io.github.terrence721.saga.user.grpc.LoginResponse;
import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc;
import io.github.terrence721.saga.user.grpc.ValidateTokenRequest;
import io.github.terrence721.saga.user.grpc.ValidateTokenResponse;
import io.github.terrence721.saga.user.infra.security.JwtTokenProvider;
import io.github.terrence721.saga.user.repository.UserRepository;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserGrpcServiceIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private Server server;
    private ManagedChannel channel;
    private UserIdentityServiceGrpc.UserIdentityServiceBlockingStub client;

    @BeforeEach
    void startInProcessServer() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new UserGrpcServiceImpl(userRepository, passwordEncoder, jwtTokenProvider))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        client = UserIdentityServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopInProcessServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void loginOverGrpcReturnsAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("hashed-password")
                .active(true)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.createToken(user)).thenReturn(new JwtTokenProvider.IssuedToken("signed-token", 3600L));

        LoginResponse response = client.login(LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("correct-password")
                .build());

        assertThat(response.getUserId()).isEqualTo(userId.toString());
        assertThat(response.getAccessToken()).isEqualTo("signed-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void loginOverGrpcPropagatesNotFoundStatus() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.newBuilder()
                .setEmail("missing@example.com")
                .setPassword("irrelevant")
                .build();

        assertThatThrownBy(() -> client.login(request))
                .isInstanceOf(StatusRuntimeException.class)
                .extracting(ex -> ((StatusRuntimeException) ex).getStatus().getCode())
                .isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void validateTokenOverGrpcReturnsValid() {
        UUID userId = UUID.randomUUID();

        Claim userIdClaim = Mockito.mock(Claim.class);
        when(userIdClaim.asString()).thenReturn(userId.toString());
        DecodedJWT decodedJWT = Mockito.mock(DecodedJWT.class);
        when(decodedJWT.getClaim("user-id")).thenReturn(userIdClaim);
        when(jwtTokenProvider.verifyToken("valid-token")).thenReturn(Optional.of(decodedJWT));

        ValidateTokenResponse response = client.validateToken(ValidateTokenRequest.newBuilder()
                .setAccessToken("valid-token")
                .build());

        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(userId.toString());
    }
}
