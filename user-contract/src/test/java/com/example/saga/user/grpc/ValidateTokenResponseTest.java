package com.example.saga.user.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateTokenResponseTest {

    @Test
    void defaultInstanceHasEmptyFields() {
        ValidateTokenResponse response = ValidateTokenResponse.getDefaultInstance();

        assertThat(response.getValid()).isFalse();
        assertThat(response.getUserId()).isEmpty();
        assertThat(response.isInitialized()).isTrue();
    }

    @Test
    void builderSetsFields() {
        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
                .build();

        assertThat(response.getValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo("u");
    }

    @Test
    void builderClearsFields() {
        ValidateTokenResponse cleared = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
                .clearValid()
                .clearUserId()
                .build();

        assertThat(cleared.getValid()).isFalse();
        assertThat(cleared.getUserId()).isEmpty();
    }

    @Test
    void builderBytesSetterWorks() {
        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setUserIdBytes(com.google.protobuf.ByteString.copyFromUtf8("u"))
                .build();

        assertThat(response.getUserId()).isEqualTo("u");
    }

    @Test
    void newBuilderFromPrototypeCopiesFields() {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
                .build();

        assertThat(ValidateTokenResponse.newBuilder(original).build()).isEqualTo(original);
    }

    @Test
    void mergeFromCopiesFields() {
        ValidateTokenResponse merged = ValidateTokenResponse.newBuilder()
                .setValid(false)
                .mergeFrom(ValidateTokenResponse.newBuilder().setValid(true).setUserId("u").build())
                .build();

        assertThat(merged.getValid()).isTrue();
        assertThat(merged.getUserId()).isEqualTo("u");
    }

    @Test
    void equalityAndHashCodeWork() {
        ValidateTokenResponse first = ValidateTokenResponse.newBuilder().setValid(true).setUserId("u").build();
        ValidateTokenResponse second = ValidateTokenResponse.newBuilder().setValid(true).setUserId("u").build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void parseFromByteArrayRoundTrips() throws InvalidProtocolBufferException {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
                .build();

        assertThat(ValidateTokenResponse.parseFrom(original.toByteArray())).isEqualTo(original);
    }

    @Test
    void parseFromInputStreamRoundTrips() throws Exception {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder().setValid(true).build();

        assertThat(ValidateTokenResponse.parseFrom(new ByteArrayInputStream(original.toByteArray())))
                .isEqualTo(original);
    }

    @Test
    void parseDelimitedFromRoundTrips() throws Exception {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder().setValid(true).build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.writeDelimitedTo(out);

        assertThat(ValidateTokenResponse.parseDelimitedFrom(new ByteArrayInputStream(out.toByteArray())))
                .isEqualTo(original);
    }

    @Test
    void parseFromCodedInputStreamRoundTrips() throws Exception {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder().setValid(true).build();

        com.google.protobuf.CodedInputStream input =
                com.google.protobuf.CodedInputStream.newInstance(original.toByteArray());
        assertThat(ValidateTokenResponse.parseFrom(input)).isEqualTo(original);
    }

    @Test
    void writeToAndSerializedSizeMatch() {
        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
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
        assertThat(ValidateTokenResponse.getDescriptor().getName()).isEqualTo("ValidateTokenResponse");
        assertThat(ValidateTokenResponse.newBuilder().getDescriptorForType())
                .isEqualTo(ValidateTokenResponse.getDescriptor());
        assertThat(ValidateTokenResponse.newBuilder().getDefaultInstanceForType())
                .isEqualTo(ValidateTokenResponse.getDefaultInstance());
    }

    @Test
    void toStringContainsFieldValues() {
        ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
                .build();

        assertThat(response.toString()).contains("u");
    }
}
