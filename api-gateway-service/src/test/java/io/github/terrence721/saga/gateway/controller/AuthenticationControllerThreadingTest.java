package io.github.terrence721.saga.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.github.terrence721.saga.gateway.dto.AuthRequest;
import io.github.terrence721.saga.gateway.infra.grpc.UserGrpcClient;
import io.github.terrence721.saga.user.grpc.LoginResponse;

/**
 * Verifies the blocking gRPC call in AuthenticationController.login() actually runs off
 * Reactor Netty's event-loop threads. Needs a real running server (webEnvironment = RANDOM_PORT),
 * not @WebFluxTest's mock request/response binding, since the event-loop group only exists
 * once Netty is actually started.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=test-only-secret-never-used-outside-this-test"
})
class AuthenticationControllerThreadingTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserGrpcClient userGrpcClient;

    @Test
    void login_DoesNotBlockTheNettyEventLoopThread() {
        AtomicReference<String> observedThreadName = new AtomicReference<>();

        when(userGrpcClient.login(any(AuthRequest.class))).thenAnswer(invocation -> {
            observedThreadName.set(Thread.currentThread().getName());
            return LoginResponse.newBuilder()
                    .setUserId("1")
                    .setAccessToken("t")
                    .setTokenType("Bearer")
                    .setExpiresInSeconds(3600)
                    .build();
        });

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().isOk();

        assertThat(observedThreadName.get())
                .as("the blocking gRPC call must not run on a Reactor Netty event-loop thread")
                .doesNotContain("webflux-http-nio")
                .doesNotContain("reactor-http-nio");
    }
}
