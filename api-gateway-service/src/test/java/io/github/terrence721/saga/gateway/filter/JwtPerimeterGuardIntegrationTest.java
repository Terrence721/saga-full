package io.github.terrence721.saga.gateway.filter;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.beans.factory.annotation.Autowired;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

/**
 * Verifies GlobalExceptionHandler's @ExceptionHandler(JWTVerificationException.class) and
 * @ExceptionHandler(TokenExpiredException.class) actually translate a real, real-request
 * rejection from the JwtPerimeterGuard gateway filter into the documented error shapes - not
 * assumed, since @RestControllerAdvice only intercepts exceptions from @RestController dispatch,
 * and Spring Cloud Gateway's filter chain is a separate reactive pipeline that doesn't go through
 * controller dispatch at all.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=test-only-secret-never-used-outside-this-test"
})
class JwtPerimeterGuardIntegrationTest {

    private static final String TEST_SECRET = "test-only-secret-never-used-outside-this-test";
    private static final String TEST_ISSUER = "saga-ecosystem-auth";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void ordersRoute_RejectsMissingToken_WithDocumentedErrorShape() {
        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").isNumber();
    }

    @Test
    void ordersRoute_RejectsExpiredToken_WithDocumentedErrorShape() {
        String expiredToken = JWT.create()
                .withIssuer(TEST_ISSUER)
                .withClaim("user-id", "9942")
                .withIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .withExpiresAt(new Date(System.currentTimeMillis() - 60_000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        webTestClient.post()
                .uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error").isEqualTo("FORBIDDEN")
                .jsonPath("$.message").exists()
                .jsonPath("$.timestamp").isNumber();
    }
}
