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
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        GrpcExecutor.execute(responseObserver, () -> {
            String email = request.getEmail();
            if (email == null) {
                throw new IllegalArgumentException("email must not be null");
            }
            Optional<User> maybeUser = userRepository.findByEmail(email);

            // Runs exactly once on every path - found or not, active or not - so none of
            // them can be told apart by how long the response takes.
            boolean passwordMatches = passwordEncoder.matches(
                    request.getPassword(),
                    maybeUser.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH));

            User user = maybeUser.orElseThrow(
                    () -> new UserNotFoundException("No user found for email: " + request.getEmail()));

            if (!user.isActive()) {
                throw new UserInactiveException("User account is inactive: " + request.getEmail());
            }

            if (!passwordMatches) {
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
