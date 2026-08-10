package io.github.terrence721.saga.user.infra.grpc;

import io.github.terrence721.saga.user.domain.User;
import io.github.terrence721.saga.user.exception.InvalidCredentialsException;
import io.github.terrence721.saga.user.exception.UserInactiveException;
import io.github.terrence721.saga.user.exception.UserNotFoundException;
import io.github.terrence721.saga.user.grpc.LoginRequest;
import io.github.terrence721.saga.user.grpc.LoginResponse;
import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc;
import io.github.terrence721.saga.user.grpc.ValidateTokenRequest;
import io.github.terrence721.saga.user.grpc.ValidateTokenResponse;
import io.github.terrence721.saga.user.infra.security.JwtTokenProvider;
import io.github.terrence721.saga.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

@GrpcService
@Slf4j
public class UserGrpcServiceImpl extends UserIdentityServiceGrpc.UserIdentityServiceImplBase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserGrpcServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        GrpcExecutor.execute(responseObserver, () -> {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("No user found for email: " + request.getEmail()));

            if (!user.isActive()) {
                throw new UserInactiveException("User account is inactive: " + request.getEmail());
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new InvalidCredentialsException("Invalid credentials for email: " + request.getEmail());
            }

            JwtTokenProvider.IssuedToken issuedToken = jwtTokenProvider.createToken(user);

            return LoginResponse.newBuilder()
                    .setUserId(user.getId().toString())
                    .setAccessToken(issuedToken.token())
                    .setTokenType("Bearer")
                    .setExpiresInSeconds(issuedToken.expiresInSeconds())
                    .build();
        });
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
