package io.github.terrence721.saga.gateway.infra.grpc;

import org.springframework.stereotype.Component;

import io.github.terrence721.saga.gateway.dto.AuthRequest;
import io.github.terrence721.saga.user.grpc.LoginRequest;
import io.github.terrence721.saga.user.grpc.LoginResponse;
import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc.UserIdentityServiceBlockingStub;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {

    private final UserIdentityServiceBlockingStub userIdentityServiceStub;
    private final UserGrpcExceptionTranslator translator;

    public LoginResponse login(AuthRequest webAuthRequest) {

        // email is a client-controlled, content-unrestricted @RequestBody field logged on
        // an unauthenticated endpoint - needs the same CR/LF sanitizing as OrderController's
        // itemCode logging (see that class) before it's safe to log.
        String sanitizedEmail = webAuthRequest.email().replaceAll("[\r\n]", "_");

        log.info("Initiating gRPC login call for email: {}", sanitizedEmail);

        try {
            LoginRequest grpcRequest = LoginRequest.newBuilder()
                    .setEmail(webAuthRequest.email())
                    .setPassword(webAuthRequest.password())
                    .build();

            LoginResponse response = userIdentityServiceStub.login(grpcRequest);

            log.info("Authenticated user successfully for email: {}", sanitizedEmail);

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC invocation returned an error status: [{}] - Reason: {}",
                    e.getStatus().getCode(), e.getStatus().getDescription());
            throw translator.translate(e);
        }
    }
}
