package io.github.terrence721.saga.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.terrence721.saga.gateway.dto.AuthRequest;
import io.github.terrence721.saga.gateway.dto.WebTokenResponse;
import io.github.terrence721.saga.gateway.infra.grpc.UserGrpcClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserGrpcClient userGrpcClient;

    @PostMapping("/login")
    public Mono<ResponseEntity<WebTokenResponse>> login(
                @Valid @RequestBody AuthRequest request) {

        // With virtual threads enabled globally, the blocking gRPC call can run
        // inside Mono.fromCallable() without a manual scheduler override - WebFlux's
        // Netty event loop is never occupied by it.
        return Mono.fromCallable(() -> userGrpcClient.login(request))
                .map(grpcResponse ->
                    ResponseEntity.ok(
                            new WebTokenResponse(
                                    grpcResponse.getAccessToken(),
                                    grpcResponse.getTokenType(),
                                    grpcResponse.getExpiresInSeconds()
                            )
                    )
                )
                .defaultIfEmpty(ResponseEntity.status(401).build());
    }
}
