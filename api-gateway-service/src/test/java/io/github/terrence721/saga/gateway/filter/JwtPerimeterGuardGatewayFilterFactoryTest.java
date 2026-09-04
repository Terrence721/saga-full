package io.github.terrence721.saga.gateway.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class JwtPerimeterGuardGatewayFilterFactoryTest {

    private JwtPerimeterGuardGatewayFilterFactory filterFactory;
    private GatewayFilterChain filterChain;
    private ArgumentCaptor<ServerWebExchange> exchangeCaptor;

    private final String testSecret = "9f8c3b7e2d6a4f1c9b0e7d5a3c2f1e6b7a8c9d0e1f2a3b4c5d6e7f8091a2b3c4";
    private final String testIssuer = "saga-ecosystem-auth";

    @BeforeEach
    void setUp() {
        this.filterFactory = new JwtPerimeterGuardGatewayFilterFactory(testSecret, testIssuer);
        this.filterChain = mock(GatewayFilterChain.class);
        this.exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void apply_ShouldForwardAndMutateHeader_WhenTokenSignatureIsValid() {
        String validToken = JWT.create()
                .withIssuer(testIssuer)
                .withSubject("alex@example.com")
                .withClaim("user-id", "9942")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 60000))
                .sign(Algorithm.HMAC256(testSecret));

        MockServerHttpRequest request = MockServerHttpRequest.post("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new JwtPerimeterGuardGatewayFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchangeCaptor.capture());
        ServerWebExchange mutatedExchange = exchangeCaptor.getValue();

        String extractedUserHeader = mutatedExchange.getRequest().getHeaders().getFirst("X-Perimeter-User-Id");
        assertNotNull(extractedUserHeader);
        assertEquals("9942", extractedUserHeader);
    }

    /**
     * The filter itself never sets a response status - it signals an error for
     * GlobalExceptionHandler's {@code @ExceptionHandler(JWTVerificationException.class)}
     * to translate into 401 further up the chain, which isn't present in this
     * isolated unit test. So the real, verifiable contract here is: the emitted
     * error is a JWTVerificationException, and the downstream chain never runs.
     */
    @Test
    void apply_ShouldEmitJwtVerificationError_WhenAuthorizationHeaderIsMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/orders").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new JwtPerimeterGuardGatewayFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectErrorMatches(ex -> ex instanceof JWTVerificationException
                        && ex.getMessage().contains("Missing or malformed"))
                .verify();

        verifyNoInteractions(filterChain);
    }

    /**
     * A malformed/tampered token throws JWTDecodeException, a JWTVerificationException
     * subtype - so GlobalExceptionHandler's same handler still maps it to 401, not 403
     * (403 is reserved for TokenExpiredException specifically).
     */
    @Test
    void apply_ShouldEmitJwtVerificationError_WhenTokenSignatureIsInvalidOrTampered() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.tampered.token.string")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new JwtPerimeterGuardGatewayFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectError(JWTVerificationException.class)
                .verify();

        verifyNoInteractions(filterChain);
    }

    /**
     * CR/LF sanitization itself is verified statically by CodeQL's java/log-injection
     * query, matching this repo's established convention (see OrderService's
     * cancelOrder_stillCancelsOrder_whenReasonContainsCrLf). This test instead proves
     * the sanitizing replaceAll call doesn't disturb normal error handling: a token
     * whose base64url-decoded payload is invalid JSON containing forged CR/LF content
     * still surfaces as a real JWTDecodeException with the chain never invoked, exactly
     * like any other malformed token.
     */
    @Test
    void apply_ShouldStillEmitJwtVerificationError_WhenDecodedPayloadContainsCrLf() {
        String headerB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String forgedPayload = "not json\r\nFAKE LOG LINE: user 9999 authenticated successfully";
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8));
        String maliciousToken = headerB64 + "." + payloadB64 + ".fakesignature";

        MockServerHttpRequest request = MockServerHttpRequest.post("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + maliciousToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter filter = filterFactory.apply(new JwtPerimeterGuardGatewayFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .expectError(JWTVerificationException.class)
                .verify();

        verifyNoInteractions(filterChain);
    }
}
