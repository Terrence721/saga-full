package io.github.terrence721.saga.gateway.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.github.terrence721.saga.gateway.dto.AuthRequest;
import io.github.terrence721.saga.gateway.exception.GlobalExceptionHandler;
import io.github.terrence721.saga.gateway.exception.InvalidCredentialsException;
import io.github.terrence721.saga.gateway.infra.grpc.UserGrpcClient;
import io.github.terrence721.saga.user.grpc.LoginResponse;

@WebFluxTest(
    controllers = {
        AuthenticationController.class,
        GlobalExceptionHandler.class
    }
)
class AuthenticationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserGrpcClient userGrpcClient;

    @Test
    void login_ShouldReturnOkAndToken_WhenCredentialsAreValid() {
        LoginResponse grpcMockResponse = LoginResponse.newBuilder()
                .setUserId("9942")
                .setAccessToken("mocked-valid-jwt-token-string")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600)
                .build();

        when(userGrpcClient.login(any(AuthRequest.class))).thenReturn(grpcMockResponse);

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.token").isEqualTo("mocked-valid-jwt-token-string")
                .jsonPath("$.type").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isEqualTo(3600);
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenInvalidCredentialsExceptionIsThrown() {
        when(userGrpcClient.login(any(AuthRequest.class)))
                .thenThrow(new InvalidCredentialsException("Cryptographic credential check failed"));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "wrongPassword"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message").isEqualTo("Cryptographic credential check failed");
    }

    @Test
    void login_ShouldReturnBadRequest_WhenPayloadInputFailsValidationRules() {
        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("", ""))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
