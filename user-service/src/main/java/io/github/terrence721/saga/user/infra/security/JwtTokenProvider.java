package io.github.terrence721.saga.user.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.github.terrence721.saga.user.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private final Algorithm algorithm;
    private final String issuer;
    private final long expirationMs;
    private final JWTVerifier verifier;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirationMs = expirationMs;
        // Built once, matching api-gateway-service's JwtPerimeterGuardGatewayFilterFactory -
        // algorithm/issuer never change after construction, so there's nothing to rebuild.
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public IssuedToken createToken(User user) {
        Date now = new Date();
        @SuppressWarnings("null") // User.email is a NOT NULL DB column; never null once loaded.
        String email = user.getEmail();
        String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(email)
                .withClaim("user-id", user.getId().toString())
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expirationMs))
                .sign(algorithm);

        return new IssuedToken(token, expirationMs / 1000);
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, long expiresInSeconds) {}
}
