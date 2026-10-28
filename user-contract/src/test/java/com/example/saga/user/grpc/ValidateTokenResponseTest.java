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


    @Test
    void parseFromInputStreamWithRegistryRoundTrips() throws Exception {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder()
                .setValid(true)
                .setUserId("u")
                .build();
        ValidateTokenResponse parsed = ValidateTokenResponse.parseFrom(
                new java.io.ByteArrayInputStream(original.toByteArray()),
                com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromByteStringRoundTrips() throws com.google.protobuf.InvalidProtocolBufferException {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder().setValid(true).build();
        ValidateTokenResponse parsed = ValidateTokenResponse.parseFrom(com.google.protobuf.ByteString.copyFrom(original.toByteArray()));
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseDelimitedFromWithRegistryRoundTrips() throws Exception {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder().setValid(true).build();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        original.writeDelimitedTo(out);
        ValidateTokenResponse parsed = ValidateTokenResponse.parseDelimitedFrom(
                new java.io.ByteArrayInputStream(out.toByteArray()),
                com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromParserRoundTrips() throws Exception {
        ValidateTokenResponse original = ValidateTokenResponse.newBuilder().setValid(true).build();
        com.google.protobuf.Parser<ValidateTokenResponse> parser = ValidateTokenResponse.parser();
        ValidateTokenResponse parsed = parser.parseFrom(original.toByteArray());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void hashCodeVariesWithFields() {
        ValidateTokenResponse first = ValidateTokenResponse.newBuilder().setValid(true).setUserId("u").build();
        ValidateTokenResponse second = ValidateTokenResponse.newBuilder().setValid(true).setUserId("u").build();
        ValidateTokenResponse empty = ValidateTokenResponse.getDefaultInstance();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(empty.hashCode()).isNotEqualTo(first.hashCode());
    }

    @Test
    void equalsHandlesMismatchedAndEmpty() {
        ValidateTokenResponse first = ValidateTokenResponse.newBuilder().setValid(true).setUserId("u").build();
        assertThat(first.equals(first)).isTrue();
        assertThat(first.equals(null)).isFalse();
        assertThat(first.equals("not-a-message")).isFalse();
        assertThat(ValidateTokenResponse.getDefaultInstance().equals(ValidateTokenResponse.getDefaultInstance())).isTrue();
    }

    @Test
    void toStringHandlesEmptyInstance() {
        assertThat(ValidateTokenResponse.getDefaultInstance().toString()).isNotNull();
    }

    @Test
    void invalidParseThrows() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                ValidateTokenResponse.parseFrom(new byte[]{(byte) 0xff}))
                .isInstanceOf(com.google.protobuf.InvalidProtocolBufferException.class);
    }
}
