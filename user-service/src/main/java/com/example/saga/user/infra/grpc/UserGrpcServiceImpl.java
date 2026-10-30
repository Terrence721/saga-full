package com.example.saga.user.infra.grpc;

import com.example.saga.user.domain.User;
import com.example.saga.user.exception.InvalidCredentialsException;
import com.example.saga.user.exception.UserInactiveException;
import com.example.saga.user.exception.UserNotFoundException;
import com.example.saga.user.grpc.UserIdentityServiceGrpc;
import com.example.saga.user.grpc.ValidateTokenRequest;
import com.example.saga.user.grpc.ValidateTokenResponse;
import com.example.saga.user.infra.security.JwtTokenProvider;
import com.example.saga.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@GrpcService
@Slf4j
public class UserGrpcServiceImpl extends UserIdentityServiceGrpc.UserIdentityServiceImplBase {

    // A valid but unused bcrypt hash, compared against whenever no real user is found -
    // so an unknown email costs the same real BCrypt verification time as a known one,
    // rather than returning early and leaking "this account doesn't exist" via response
    // latency even after GrpcExecutor already hides it behind a generic status.
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$Iwf4.JZgaIfsOqlfbLfQg.sS6S9u0Dg8lzeHjeMRivBKrL8mW5Dv6";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserGrpcServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
