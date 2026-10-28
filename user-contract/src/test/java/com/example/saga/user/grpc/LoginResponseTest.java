package com.example.saga.user.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseTest {

    @Test
    void defaultInstanceHasEmptyFields() {
        LoginResponse response = LoginResponse.getDefaultInstance();

        assertThat(response.getUserId()).isEmpty();
        assertThat(response.getAccessToken()).isEmpty();
        assertThat(response.getTokenType()).isEmpty();
        assertThat(response.getExpiresInSeconds()).isZero();
        assertThat(response.isInitialized()).isTrue();
    }

    @Test
    void builderSetsAndClearsFields() {
        LoginResponse response = LoginResponse.newBuilder()
                .setUserId("u")
                .setAccessToken("tok")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600L)
                .build();

        assertThat(response.getUserId()).isEqualTo("u");
        assertThat(response.getAccessToken()).isEqualTo("tok");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInSeconds()).isEqualTo(3600L);

        LoginResponse cleared = response.toBuilder()
                .clearUserId().clearAccessToken().clearTokenType().clearExpiresInSeconds()
                .build();
        assertThat(cleared.getUserId()).isEmpty();
        assertThat(cleared.getAccessToken()).isEmpty();
        assertThat(cleared.getTokenType()).isEmpty();
        assertThat(cleared.getExpiresInSeconds()).isZero();
    }

    @Test
    void builderBytesSettersWork() {
        LoginResponse response = LoginResponse.newBuilder()
                .setUserIdBytes(com.google.protobuf.ByteString.copyFromUtf8("u"))
                .setAccessTokenBytes(com.google.protobuf.ByteString.copyFromUtf8("tok"))
                .setTokenTypeBytes(com.google.protobuf.ByteString.copyFromUtf8("Bearer"))
                .build();

        assertThat(response.getUserId()).isEqualTo("u");
        assertThat(response.getAccessToken()).isEqualTo("tok");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void newBuilderFromPrototypeCopiesFields() {
        LoginResponse original = LoginResponse.newBuilder()
                .setUserId("u")
                .setExpiresInSeconds(Long.MAX_VALUE)
                .build();

        assertThat(LoginResponse.newBuilder(original).build()).isEqualTo(original);
    }

    @Test
    void mergeFromCopiesFields() {
        LoginResponse merged = LoginResponse.newBuilder()
                .setUserId("old")
                .mergeFrom(LoginResponse.newBuilder().setAccessToken("tok").setExpiresInSeconds(5L).build())
                .build();

        assertThat(merged.getUserId()).isEqualTo("old");
        assertThat(merged.getAccessToken()).isEqualTo("tok");
        assertThat(merged.getExpiresInSeconds()).isEqualTo(5L);
    }

    @Test
    void equalityAndHashCodeWork() {
        LoginResponse first = LoginResponse.newBuilder().setUserId("u").setExpiresInSeconds(1L).build();
        LoginResponse second = LoginResponse.newBuilder().setUserId("u").setExpiresInSeconds(1L).build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void parseFromByteArrayRoundTrips() throws InvalidProtocolBufferException {
        LoginResponse original = LoginResponse.newBuilder()
                .setUserId("u")
                .setAccessToken("tok")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600L)
                .build();

        assertThat(LoginResponse.parseFrom(original.toByteArray())).isEqualTo(original);
    }

    @Test
    void parseFromInputStreamRoundTrips() throws Exception {
        LoginResponse original = LoginResponse.newBuilder().setUserId("u").build();

        assertThat(LoginResponse.parseFrom(new ByteArrayInputStream(original.toByteArray())))
                .isEqualTo(original);
    }

    @Test
    void parseDelimitedFromRoundTrips() throws Exception {
        LoginResponse original = LoginResponse.newBuilder().setUserId("u").build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.writeDelimitedTo(out);

        assertThat(LoginResponse.parseDelimitedFrom(new ByteArrayInputStream(out.toByteArray())))
                .isEqualTo(original);
    }

    @Test
    void parseFromCodedInputStreamRoundTrips() throws Exception {
        LoginResponse original = LoginResponse.newBuilder().setUserId("u").build();

        com.google.protobuf.CodedInputStream input =
                com.google.protobuf.CodedInputStream.newInstance(original.toByteArray());
        assertThat(LoginResponse.parseFrom(input)).isEqualTo(original);
    }

    @Test
    void writeToAndSerializedSizeMatch() {
        LoginResponse response = LoginResponse.newBuilder()
                .setUserId("u")
                .setAccessToken("tok")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600L)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            response.writeTo(out);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        assertThat(out.toByteArray()).isEqualTo(response.toByteArray());
        assertThat(response.getSerializedSize()).isEqualTo(out.size());
    }

    @Test
    void descriptorAccessorsWork() {
        assertThat(LoginResponse.getDescriptor().getName()).isEqualTo("LoginResponse");
        assertThat(LoginResponse.newBuilder().getDescriptorForType())
                .isEqualTo(LoginResponse.getDescriptor());
        assertThat(LoginResponse.newBuilder().getDefaultInstanceForType())
                .isEqualTo(LoginResponse.getDefaultInstance());
    }

    @Test
    void toStringContainsFieldValues() {
        LoginResponse response = LoginResponse.newBuilder()
                .setUserId("u")
                .setAccessToken("tok")
                .build();

        assertThat(response.toString()).contains("u");
        assertThat(response.toString()).contains("tok");
    }
}
