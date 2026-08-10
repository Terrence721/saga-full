package io.github.terrence721.saga.user.infra.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.terrence721.saga.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private User sampleUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .passwordHash("irrelevant-for-this-test")
                .active(true)
                .build();
    }

    @Test
    void createdTokenVerifiesSuccessfullyWithMatchingClaims() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret", "saga-test-issuer", 3_600_000L);
        User user = sampleUser();

        JwtTokenProvider.IssuedToken issued = provider.createToken(user);
        Optional<DecodedJWT> decoded = provider.verifyToken(issued.token());

        assertThat(issued.expiresInSeconds()).isEqualTo(3600L);
        assertThat(decoded).isPresent();
        assertThat(decoded.get().getSubject()).isEqualTo(user.getEmail());
        assertThat(decoded.get().getClaim("user-id").asString()).isEqualTo(user.getId().toString());
        assertThat(decoded.get().getIssuer()).isEqualTo("saga-test-issuer");
    }

    @Test
    void verifyTokenReturnsEmptyForTamperedSignature() {
        JwtTokenProvider issuingProvider = new JwtTokenProvider("correct-secret", "saga-test-issuer", 3_600_000L);
        JwtTokenProvider verifyingProvider = new JwtTokenProvider("wrong-secret", "saga-test-issuer", 3_600_000L);

        JwtTokenProvider.IssuedToken issued = issuingProvider.createToken(sampleUser());

        assertThat(verifyingProvider.verifyToken(issued.token())).isEmpty();
    }

    @Test
    void verifyTokenReturnsEmptyForWrongIssuer() {
        JwtTokenProvider issuingProvider = new JwtTokenProvider("shared-secret", "issuer-a", 3_600_000L);
        JwtTokenProvider verifyingProvider = new JwtTokenProvider("shared-secret", "issuer-b", 3_600_000L);

        JwtTokenProvider.IssuedToken issued = issuingProvider.createToken(sampleUser());

        assertThat(verifyingProvider.verifyToken(issued.token())).isEmpty();
    }

    @Test
    void verifyTokenReturnsEmptyForExpiredToken() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret", "saga-test-issuer", -60_000L);

        JwtTokenProvider.IssuedToken issued = provider.createToken(sampleUser());

        assertThat(provider.verifyToken(issued.token())).isEmpty();
    }

    @Test
    void verifyTokenReturnsEmptyForMalformedToken() {
        JwtTokenProvider provider = new JwtTokenProvider("test-secret", "saga-test-issuer", 3_600_000L);

        assertThat(provider.verifyToken("not-a-real-jwt")).isEmpty();
    }
}
