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
 * Verifies GlobalCorsConfig's CorsWebFilter is actually wired, not just declared - a
 * preflight request is handled by a filter chain that runs before route matching/
 * JwtPerimeterGuard, so this is the only way to prove it works without a browser: nothing
 * else in this repo sends a real OPTIONS preflight.
 *
 * <p>{@code /auth/login} specifically is a real regression test, not just extra coverage:
 * before {@link GlobalCorsConfig} existed, {@code spring.cloud.gateway.server.webflux.
 * globalcors} was the only CORS mechanism, wired solely into RoutePredicateHandlerMapping
 * (the mapping that resolves this app's YAML-defined routes) - it had no effect on
 * {@code /auth/login}, served by a real local {@code @RestController}
 * (AuthenticationController) through a completely different HandlerMapping with no CORS
 * configuration of its own. Confirmed for real against a running container: that preflight
 * returned 403 while the identical origin worked fine on a Gateway-routed path.
 * CorsWebFilter, being a HandlerMapping-agnostic WebFilter, fixes this for every path
 * uniformly.
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

    private static final String FRONTEND_ORIGIN = "http://localhost:5180";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void preflightRequest_fromFrontendOrigin_getsAllowedOnOrdersRoute() {
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
        // Same CorsWebFilter applies app-wide, not per-route - one check on the SSE
        // route is enough to prove it isn't scoped in a way that misses it.
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
    void preflightRequest_fromFrontendOrigin_isAllowedOnAuthLoginRoute_realRegressionCase() {
        // /auth/login is served by a real local @RestController (AuthenticationController),
        // not proxied to another service like the /orders routes - a completely different
        // HandlerMapping than the one globalcors used to wire into exclusively. This is the
        // exact path that returned 403 before GlobalCorsConfig's CorsWebFilter was added.
        webTestClient.options()
                .uri("http://localhost/auth/login")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
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
