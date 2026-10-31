package com.example.saga.gateway.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Optional fine-grained authorization filter.
 *
 * Configure on a route with:
 * filters:
 *   - RoleAuthorization=ADMIN,OPERATOR
 *
 * The required roles are read from the token's `roles` claim (comma-delimited
 * or JSON-array-friendly after trimming) or `scope`/`scp` claims.
 */
@Slf4j
@Component
public class RoleAuthorizationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RoleAuthorizationGatewayFilterFactory.Config> {

    private final List<JWTVerifier> verifiers;

    public RoleAuthorizationGatewayFilterFactory(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.previous-secrets:}") String previousSecrets) {

        super(Config.class);

        List<String> secrets = new ArrayList<>();
        secrets.add(secret);
        if (previousSecrets != null && !previousSecrets.isBlank()) {
            Arrays.stream(previousSecrets.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(secrets::add);
        }

        this.verifiers = new ArrayList<>();
        for (String value : secrets) {
            Algorithm algorithm = Algorithm.HMAC256(value);
            this.verifiers.add(JWT.require(algorithm).withIssuer(issuer).build());
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        List<String> requiredRoles = config.getRoles() == null || config.getRoles().isBlank()
                ? List.of()
                : Arrays.stream(config.getRoles().split(","))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .toList();

        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token"));
            }

            String token = authHeader.substring(7);
            DecodedJWT jwt = null;
            for (JWTVerifier verifier : verifiers) {
                try {
                    jwt = verifier.verify(token);
                    break;
                } catch (Exception ignored) {
                    // Try the next configured key for graceful rotation.
                }
            }

            if (jwt == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
            }

            List<String> granted = new ArrayList<>();
            String rolesClaim = jwt.getClaim("roles").asString();
            if (rolesClaim != null && !rolesClaim.isBlank()) {
                Arrays.stream(rolesClaim.split("[,\s]+"))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .forEach(granted::add);
            }
            String scopeClaim = jwt.getClaim("scope").asString();
            if (scopeClaim != null && !scopeClaim.isBlank()) {
                Arrays.stream(scopeClaim.split("[,\s]+"))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .forEach(granted::add);
            }

            boolean allowed = requiredRoles.isEmpty() || granted.stream().anyMatch(requiredRoles::contains);
            if (!allowed) {
                log.warn("Authorization denied: required={}, granted={}", requiredRoles, granted);
                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role"));
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        private String roles = "";

        public String getRoles() {
            return roles;
        }

        public void setRoles(String roles) {
            this.roles = roles;
        }
    }
}
