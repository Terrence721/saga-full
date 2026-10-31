package com.example.saga.user.infra.grpc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.saga.user.domain.User;
import com.example.saga.user.grpc.ValidateTokenRequest;
import com.example.saga.user.grpc.ValidateTokenResponse;
import com.example.saga.user.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserGrpcServiceImplTest {

    private static final String SECRET = "test-secret";
    private static final String ISSUER = "saga-test-issuer";

    private String mintToken(long expirationMs, User user) {
        Date now = new Date();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withClaim("user-id", user.getId().toString())
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expirationMs))
                .sign(Algorithm.HMAC256(SECRET));
    }

    @Test
    void validateToken_returnsValidWithUserIdForValidToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ISSUER);
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .active(true)
                .build();
        String token = mintToken(3_600_000L, user);

        RecordingStreamObserver<ValidateTokenResponse> observer = new RecordingStreamObserver<>();
        UserGrpcServiceImpl service = new UserGrpcServiceImpl(provider);

        service.validateToken(ValidateTokenRequest.newBuilder().setAccessToken(token).build(), observer);

        ValidateTokenResponse response = observer.firstValue();
        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(user.getId().toString());
        assertThat(observer.error()).isNull();
    }

    @Test
    void validateToken_returnsInvalidForTamperedToken() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, ISSUER);

        RecordingStreamObserver<ValidateTokenResponse> observer = new RecordingStreamObserver<>();
        UserGrpcServiceImpl service = new UserGrpcServiceImpl(provider);

        service.validateToken(ValidateTokenRequest.newBuilder().setAccessToken("not-a-token").build(), observer);

        ValidateTokenResponse response = observer.firstValue();
        assertThat(response.getValid()).isFalse();
        assertThat(observer.error()).isNull();
    }
}
