package io.github.terrence721.saga.user.infra.security;

import com.auth0.jwt.JWT;
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

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirationMs = expirationMs;
    }

    public IssuedToken createToken(User user) {
        Date now = new Date();
        String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getEmail())
                .withClaim("user-id", user.getId().toString())
                .withIssuedAt(now)
                .withExpiresAt(new Date(now.getTime() + expirationMs))
                .sign(algorithm);

        return new IssuedToken(token, expirationMs / 1000);
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        try {
            return Optional.of(JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, long expiresInSeconds) {}
}
