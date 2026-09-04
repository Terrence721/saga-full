package io.github.terrence721.saga.gateway.infra.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.terrence721.saga.gateway.dto.AuthRequest;
import io.github.terrence721.saga.gateway.exception.InvalidCredentialsException;
import io.github.terrence721.saga.user.grpc.LoginRequest;
import io.github.terrence721.saga.user.grpc.LoginResponse;
import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc.UserIdentityServiceBlockingStub;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class UserGrpcClientTest {

    private UserIdentityServiceBlockingStub stub;
    private UserGrpcExceptionTranslator translator;
    private UserGrpcClient client;

    @BeforeEach
    void setUp() {
        stub = mock(UserIdentityServiceBlockingStub.class);
        translator = mock(UserGrpcExceptionTranslator.class);
        client = new UserGrpcClient(stub, translator);
    }

    @Test
    void login_ReturnsTheStubResponse_BuildingTheGrpcRequestFromEmailAndPassword() {
        AuthRequest webAuthRequest = new AuthRequest("alex@example.com", "correct-horse");
        LoginResponse expectedResponse = LoginResponse.newBuilder()
                .setUserId("9942")
                .setAccessToken("a-real-jwt")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600)
                .build();
        when(stub.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        LoginResponse actualResponse = client.login(webAuthRequest);

        assertThat(actualResponse).isEqualTo(expectedResponse);

        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(stub).login(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("alex@example.com");
        assertThat(requestCaptor.getValue().getPassword()).isEqualTo("correct-horse");
    }

    @Test
    void login_ThrowsTheTranslatedException_WhenTheStubReturnsAGrpcError() {
        AuthRequest webAuthRequest = new AuthRequest("alex@example.com", "wrong-password");
        StatusRuntimeException grpcError = Status.UNAUTHENTICATED
                .withDescription("Invalid email or password")
                .asRuntimeException();
        when(stub.login(any(LoginRequest.class))).thenThrow(grpcError);
        InvalidCredentialsException translated = new InvalidCredentialsException("Invalid email or password provided");
        when(translator.translate(grpcError)).thenReturn(translated);

        assertThatThrownBy(() -> client.login(webAuthRequest))
                .isSameAs(translated);

        verify(translator).translate(grpcError);
    }

    /**
     * CR/LF sanitization itself is verified statically by CodeQL's java/log-injection
     * query, matching this repo's established convention (see OrderService's
     * cancelOrder_stillCancelsOrder_whenReasonContainsCrLf). This test instead proves
     * the sanitizing replaceAll call doesn't disturb normal processing: a login whose
     * email contains forged CR/LF still builds the correct gRPC request and returns
     * the stub's real response.
     */
    @Test
    void login_StillSucceeds_WhenEmailContainsCrLf() {
        String forgedEmail = "alex@example.com\r\nFAKE LOG LINE: admin authenticated successfully";
        AuthRequest webAuthRequest = new AuthRequest(forgedEmail, "correct-horse");
        LoginResponse expectedResponse = LoginResponse.newBuilder()
                .setUserId("9942")
                .setAccessToken("a-real-jwt")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600)
                .build();
        when(stub.login(any(LoginRequest.class))).thenReturn(expectedResponse);

        LoginResponse actualResponse = client.login(webAuthRequest);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        verify(stub).login(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo(forgedEmail);
    }
}
