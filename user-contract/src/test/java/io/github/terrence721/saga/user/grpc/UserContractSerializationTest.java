package io.github.terrence721.saga.user.grpc;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class UserContractSerializationTest {

    @Test
    void loginRequestRoundTripsThroughSerialization() throws IOException {
        LoginRequest original = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        LoginRequest parsed = LoginRequest.parseFrom(original.toByteArray());

        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getEmail()).isEqualTo("user@example.com");
        assertThat(parsed.getPassword()).isEqualTo("hunter2");
    }

    @Test
    void loginResponseRoundTripsThroughSerialization() throws IOException {
        LoginResponse original = LoginResponse.newBuilder()
                .setUserId("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
                .setAccessToken("eyJhbGciOiJIUzI1NiJ9.example.signature")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600L)
                .build();

        LoginResponse parsed = LoginResponse.parseFrom(original.toByteArray());

        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getExpiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void loginResponseRoundTripsAtInt64Boundary() throws IOException {
        LoginResponse original = LoginResponse.newBuilder()
                .setExpiresInSeconds(Long.MAX_VALUE)
                .build();

        LoginResponse parsed = LoginResponse.parseFrom(original.toByteArray());

        assertThat(parsed.getExpiresInSeconds()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void validateTokenRequestRoundTripsThroughSerialization() throws IOException {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder()
                .setAccessToken("eyJhbGciOiJIUzI1NiJ9.example.signature")
                .build();

        ValidateTokenRequest parsed = ValidateTokenRequest.parseFrom(original.toByteArray());

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void validateTokenResponseRoundTripsWhenValid() throws IOException {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
                .build();

        ValidateTokenResponse parsed = ValidateTokenResponse.parseFrom(original.toByteArray());

        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getValid()).isTrue();
    }

    @Test
    void validateTokenResponseRoundTripsWhenInvalid() throws IOException {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder()
                .setValid(false)
                .build();

        ValidateTokenResponse parsed = ValidateTokenResponse.parseFrom(original.toByteArray());

        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getValid()).isFalse();
        assertThat(parsed.getUserId()).isEmpty();
    }
}
