package com.example.saga.user.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JwtTokenProvider {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer) {
        this.algorithm = Algorithm.HMAC256(secret);
        // Built once, matching api-gateway-service's JwtPerimeterGuardGatewayFilterFactory -
        // algorithm/issuer never change after construction, so there's nothing to rebuild.
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

}
