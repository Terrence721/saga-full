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
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserGrpcClient userGrpcClient;

    @PostMapping("/login")
    public Mono<ResponseEntity<WebTokenResponse>> login(
                @Valid @RequestBody AuthRequest request) {

        // userGrpcClient.login() is a blocking gRPC call. spring.threads.virtual.enabled
        // does not touch Reactor Netty's event-loop group, so without subscribeOn here
        // it would run directly on a webflux-http-nio-* thread and block it for the
        // call's duration - boundedElastic isolates it instead.
        return Mono.fromCallable(() -> userGrpcClient.login(request))
                .subscribeOn(Schedulers.boundedElastic())
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
