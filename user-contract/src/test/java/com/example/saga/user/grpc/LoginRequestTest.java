package com.example.saga.user.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void defaultInstanceHasEmptyFields() {
        LoginRequest request = LoginRequest.getDefaultInstance();

        assertThat(request.getEmail()).isEmpty();
        assertThat(request.getPassword()).isEmpty();
        assertThat(request.getSerializedSize()).isZero();
        assertThat(request.isInitialized()).isTrue();
    }

    @Test
    void builderSetsAndClearsFields() {
        LoginRequest request = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getPassword()).isEqualTo("hunter2");
        assertThat(request.getEmailBytes().toStringUtf8()).isEqualTo("user@example.com");
        assertThat(request.getPasswordBytes().toStringUtf8()).isEqualTo("hunter2");

        LoginRequest cleared = request.toBuilder().clearEmail().clearPassword().build();
        assertThat(cleared.getEmail()).isEmpty();
        assertThat(cleared.getPassword()).isEmpty();
    }

    @Test
    void builderSetBytesFields() {
        LoginRequest request = LoginRequest.newBuilder()
                .setEmailBytes(com.google.protobuf.ByteString.copyFromUtf8("a@b.com"))
                .setPasswordBytes(com.google.protobuf.ByteString.copyFromUtf8("pwd"))
                .build();

        assertThat(request.getEmail()).isEqualTo("a@b.com");
        assertThat(request.getPassword()).isEqualTo("pwd");
    }

    @Test
    void newBuilderFromPrototypeCopiesFields() {
        LoginRequest original = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        LoginRequest copy = LoginRequest.newBuilder(original).build();

        assertThat(copy).isEqualTo(original);
    }

    @Test
    void mergeFromCopiesIntoExistingBuilder() {
        LoginRequest merged = LoginRequest.newBuilder()
                .setEmail("old@example.com")
                .mergeFrom(LoginRequest.newBuilder()
                        .setPassword("secret")
                        .build())
                .build();

        assertThat(merged.getEmail()).isEqualTo("old@example.com");
        assertThat(merged.getPassword()).isEqualTo("secret");
    }

    @Test
    void equalityAndHashCodeWork() {
        LoginRequest first = LoginRequest.newBuilder()
                .setEmail("u")
                .setPassword("p")
                .build();
        LoginRequest second = LoginRequest.newBuilder()
                .setEmail("u")
                .setPassword("p")
                .build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void parseFromByteArrayRoundTrips() throws InvalidProtocolBufferException {
        LoginRequest original = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        LoginRequest parsed = LoginRequest.parseFrom(original.toByteArray());

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromInputStreamRoundTrips() throws Exception {
        LoginRequest original = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        LoginRequest parsed = LoginRequest.parseFrom(new ByteArrayInputStream(original.toByteArray()));

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseDelimitedFromRoundTrips() throws Exception {
        LoginRequest original = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.writeDelimitedTo(out);

        LoginRequest parsed = LoginRequest.parseDelimitedFrom(new ByteArrayInputStream(out.toByteArray()));

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromCodedInputStreamRoundTrips() throws Exception {
        LoginRequest original = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        com.google.protobuf.CodedInputStream input =
                com.google.protobuf.CodedInputStream.newInstance(original.toByteArray());
        LoginRequest parsed = LoginRequest.parseFrom(input);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void writeToAndSerializedSizeMatch() {
        LoginRequest request = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            request.writeTo(out);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        assertThat(out.toByteArray()).isEqualTo(request.toByteArray());
        assertThat(request.getSerializedSize()).isEqualTo(out.size());
    }

    @Test
    void descriptorAccessorsWork() {
        assertThat(LoginRequest.getDescriptor().getName()).isEqualTo("LoginRequest");
        assertThat(LoginRequest.newBuilder().getDescriptorForType()).isEqualTo(LoginRequest.getDescriptor());
        assertThat(LoginRequest.newBuilder().getDefaultInstanceForType()).isEqualTo(LoginRequest.getDefaultInstance());
    }

    @Test
    void toStringContainsFieldValues() {
        LoginRequest request = LoginRequest.newBuilder()
                .setEmail("user@example.com")
                .setPassword("hunter2")
                .build();

        String text = request.toString();

        assertThat(text).contains("user@example.com");
        assertThat(text).contains("hunter2");
    }


    @Test
    void initializeMessageReturnsSelf() {
        LoginRequest request = LoginRequest.newBuilder().setEmail("x").build();
        assertThat(request.isInitialized()).isTrue();
    }
}
