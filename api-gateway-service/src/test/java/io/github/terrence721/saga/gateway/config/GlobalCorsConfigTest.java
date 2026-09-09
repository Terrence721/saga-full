package io.github.terrence721.saga.gateway.config;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies application.yaml's spring.cloud.gateway.server.webflux.globalcors block is
 * actually wired, not just declared - a preflight request is handled by a filter chain
 * that runs before route matching/JwtPerimeterGuard, so this is the only way to prove it
 * works without a browser: nothing else in this repo sends a real OPTIONS preflight.
 *
 * <p>Requests here use an absolute URI (http://localhost/...) rather than a bare path.
 * @AutoConfigureWebTestClient binds this module's WebTestClient straight to the application
 * context (MockServerHttpRequest), not a real socket, confirmed by inspecting the exchange
 * at request time rather than assumed - a request built from a relative path carries no
 * scheme/host, which Spring's reactive DefaultCorsProcessor cannot evaluate
 * (CorsUtils.isSameOrigin asserts both are present) and rejects outright, independent of
 * anything this test means to verify.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=test-only-secret-never-used-outside-this-test"
})
@AutoConfigureWebTestClient(timeout = "PT15S")
class GlobalCorsConfigTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void preflightRequest_fromFrontendOrigin_getsAllowedByGlobalCorsConfig() {
        webTestClient.options()
                .uri("http://localhost/orders")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
    }

    @Test
    void preflightRequest_fromFrontendOrigin_isAllowedOnOrderStreamRoute() {
        // Same globalcors config applies gateway-wide, not per-route - one check on the
        // new SSE route is enough to prove it isn't scoped in a way that misses it.
        webTestClient.options()
                .uri("http://localhost/orders/{id}/stream", UUID.randomUUID())
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN);
    }

    @Test
    void preflightRequest_fromDisallowedOrigin_getsNoAccessControlAllowOriginHeader() {
        webTestClient.options()
                .uri("http://localhost/orders")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }
}
