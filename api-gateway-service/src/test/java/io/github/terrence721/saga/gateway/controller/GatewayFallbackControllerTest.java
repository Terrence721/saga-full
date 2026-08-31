package io.github.terrence721.saga.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.beans.factory.annotation.Autowired;

@WebFluxTest(controllers = GatewayFallbackController.class)
class GatewayFallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void orderFallback_ReturnsServiceUnavailableWithErrorBody() {
        webTestClient.post()
                .uri("/fallback/orders")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.error").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isNotEmpty()
                .jsonPath("$.timestamp").isNumber();
    }
}
