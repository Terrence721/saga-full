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
import io.github.terrence721.saga.gateway.exception.DependencyUnavailableException;
import io.github.terrence721.saga.gateway.exception.GlobalExceptionHandler;
import io.github.terrence721.saga.gateway.exception.InvalidCredentialsException;
import io.github.terrence721.saga.gateway.exception.UserInactiveException;
import io.github.terrence721.saga.gateway.exception.UserNotFoundException;
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

    @Test
    void login_ShouldReturnNotFound_WhenUserNotFoundExceptionIsThrown() {
        when(userGrpcClient.login(any(AuthRequest.class)))
                .thenThrow(new UserNotFoundException("No user found for email: developer@saga.dev"));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("NOT_FOUND")
                .jsonPath("$.message").isEqualTo("No user found for email: developer@saga.dev");
    }

    @Test
    void login_ShouldReturnForbidden_WhenUserInactiveExceptionIsThrown() {
        when(userGrpcClient.login(any(AuthRequest.class)))
                .thenThrow(new UserInactiveException("User account is inactive: developer@saga.dev"));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error").isEqualTo("FORBIDDEN")
                .jsonPath("$.message").isEqualTo("User account is inactive: developer@saga.dev");
    }

    @Test
    void login_ShouldReturnServiceUnavailable_WhenDependencyUnavailableExceptionIsThrown() {
        when(userGrpcClient.login(any(AuthRequest.class)))
                .thenThrow(new DependencyUnavailableException(
                        "User authentication service is temporarily unavailable", new RuntimeException("boom")));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error").isEqualTo("DEPENDENCY_FAILURE")
                .jsonPath("$.message").isEqualTo("Service temporarily unavailable");
    }

    @Test
    void login_ShouldReturnBadRequest_WhenIllegalArgumentExceptionIsThrown() {
        when(userGrpcClient.login(any(AuthRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid user request fields"));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("BAD_REQUEST")
                .jsonPath("$.message").isEqualTo("Invalid user request fields");
    }

    @Test
    void login_ShouldReturnInternalServerError_WhenUnexpectedExceptionIsThrown() {
        when(userGrpcClient.login(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Something genuinely unexpected"));

        webTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequest("developer@saga.dev", "securePassword123"))
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.error").isEqualTo("INTERNAL_SERVER_ERROR")
                .jsonPath("$.message").isEqualTo("An unexpected error occurred");
    }
}
