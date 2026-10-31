package com.example.saga.user.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class JwtTokenProvider {

    private static final List<String> INSECURE_SECRETS = List.of(
            "CHANGEME", "local-dev-secret-change-me", "");

    private final List<JWTVerifier> verifiers;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.previous-secrets:}") String previousSecrets) {

        List<String> secrets = new ArrayList<>();
        secrets.add(secret);
        if (previousSecrets != null && !previousSecrets.isBlank()) {
            Arrays.stream(previousSecrets.split(","))
                    .map(value -> value.trim())
                    .filter(s -> !s.isBlank())
                    .forEach(secrets::add);
        }

        for (String value : secrets) {
            if (value == null || value.isBlank() || INSECURE_SECRETS.contains(value)
                || value.contains("CHANGEME")) {
                throw new IllegalStateException(
                        "app.jwt.secret/previous secrets must be provided via a real secret store");
            }
        }

        this.verifiers = new ArrayList<>();
        for (String value : secrets) {
            Algorithm algorithm = Algorithm.HMAC256(value);
            this.verifiers.add(JWT.require(algorithm).withIssuer(issuer).build());
        }
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        for (JWTVerifier verifier : verifiers) {
            try {
                return Optional.of(verifier.verify(token));
            } catch (JWTVerificationException ignored) {
                // Try the next configured key for graceful rotation.
            }
        }
        return Optional.empty();
    }
}
