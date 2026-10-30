package com.example.saga.user.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.saga.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private User sampleUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .active(true)
                .build();
    }

    private String mintToken(String secret, String issuer, long expirationMs, User user) {
        Date now = new Date();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getEmail())
                .withClaim("user-id", user.getId().toString())
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expirationMs))
                .sign(Algorithm.HMAC256(secret));
    }

    @Test
    void validTokenVerifiesSuccessfullyWithMatchingClaims() {
        String secret = "test-secret";
        String issuer = "saga-test-issuer";
        long expirationMs = 3_600_000L;
        JwtTokenProvider provider = new JwtTokenProvider(secret, issuer);
        User user = sampleUser();

        String token = mintToken(secret, issuer, expirationMs, user);
        Optional<DecodedJWT> decoded = provider.verifyToken(token);

        assertThat(decoded).isPresent();
        assertThat(decoded.get().getSubject()).isEqualTo(user.getEmail());
        assertThat(decoded.get().getClaim("user-id").asString()).isEqualTo(user.getId().toString());
        assertThat(decoded.get().getIssuer()).isEqualTo("saga-test-issuer");
    }

    @Test
    void verifyTokenReturnsEmptyForTamperedSignature() {
        String issuer = "saga-test-issuer";
        long expirationMs = 3_600_000L;
        JwtTokenProvider verifyingProvider = new JwtTokenProvider("wrong-secret", issuer);
        User user = sampleUser();

        String token = mintToken("correct-secret", issuer, expirationMs, user);

        assertThat(verifyingProvider.verifyToken(token)).isEmpty();
    }

    @Test
    void verifyTokenReturnsEmptyForWrongIssuer() {
        String sharedSecret = "shared-secret";
        long expirationMs = 3_600_000L;
        JwtTokenProvider verifyingProvider = new JwtTokenProvider(sharedSecret, "issuer-b");
        User user = sampleUser();

        String token = mintToken(sharedSecret, "issuer-a", expirationMs, user);

        assertThat(verifyingProvider.verifyToken(token)).isEmpty();
    }

    @Test
    void verifyTokenReturnsEmptyForExpiredToken() {
        String secret = "test-secret";
        String issuer = "saga-test-issuer";
        long expirationMs = -60_000L;
        JwtTokenProvider provider = new JwtTokenProvider(secret, issuer);
        User user = sampleUser();

        String token = mintToken(secret, issuer, expirationMs, user);

        assertThat(provider.verifyToken(token)).isEmpty();
    }

    @Test
    void verifyTokenReturnsEmptyForMalformedToken() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret", "saga-test-issuer");

        assertThat(provider.verifyToken("not-a-real-jwt")).isEmpty();
    }
}