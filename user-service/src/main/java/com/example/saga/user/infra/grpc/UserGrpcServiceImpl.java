package com.example.saga.user.infra.grpc;

import com.example.saga.user.grpc.UserIdentityServiceGrpc;
import com.example.saga.user.grpc.ValidateTokenRequest;
import com.example.saga.user.grpc.ValidateTokenResponse;
import com.example.saga.user.infra.security.JwtTokenProvider;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
public class UserGrpcServiceImpl extends UserIdentityServiceGrpc.UserIdentityServiceImplBase {

    private final JwtTokenProvider jwtTokenProvider;

    public UserGrpcServiceImpl(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        GrpcExecutor.execute(responseObserver, () ->
                jwtTokenProvider.verifyToken(request.getAccessToken())
                        .map(decoded -> ValidateTokenResponse.newBuilder()
                                .setValid(true)
                                .setUserId(decoded.getClaim("user-id").asString())
                                .build())
                        .orElseGet(() -> ValidateTokenResponse.newBuilder()
                                .setValid(false)
                                .build()));
    }
}
