package com.example.saga.user.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateTokenRequestTest {

    @Test
    void defaultInstanceHasEmptyField() {
        ValidateTokenRequest request = ValidateTokenRequest.getDefaultInstance();

        assertThat(request.getAccessToken()).isEmpty();
        assertThat(request.isInitialized()).isTrue();
        assertThat(request.getSerializedSize()).isZero();
    }

    @Test
    void builderSetsField() {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setAccessToken("token")
                .build();

        assertThat(request.getAccessToken()).isEqualTo("token");
        assertThat(request.getAccessTokenBytes().toStringUtf8()).isEqualTo("token");
    }

    @Test
    void builderSetBytesField() {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setAccessTokenBytes(com.google.protobuf.ByteString.copyFromUtf8("token"))
                .build();

        assertThat(request.getAccessToken()).isEqualTo("token");
    }

    @Test
    void builderClearsField() {
        ValidateTokenRequest cleared = ValidateTokenRequest.newBuilder()
                .setAccessToken("token")
                .clearAccessToken()
                .build();

        assertThat(cleared.getAccessToken()).isEmpty();
    }

    @Test
    void newBuilderFromPrototypeCopiesField() {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder()
                .setAccessToken("token")
                .build();

        assertThat(ValidateTokenRequest.newBuilder(original).build()).isEqualTo(original);
    }

    @Test
    void mergeFromCopiesField() {
        ValidateTokenRequest merged = ValidateTokenRequest.newBuilder()
                .setAccessToken("old")
                .mergeFrom(ValidateTokenRequest.newBuilder().setAccessToken("new").build())
                .build();

        assertThat(merged.getAccessToken()).isEqualTo("new");
    }

    @Test
    void equalityAndHashCodeWork() {
        ValidateTokenRequest first = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        ValidateTokenRequest second = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void parseFromByteArrayRoundTrips() throws InvalidProtocolBufferException {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

        assertThat(ValidateTokenRequest.parseFrom(original.toByteArray())).isEqualTo(original);
    }

    @Test
    void parseFromInputStreamRoundTrips() throws Exception {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

        assertThat(ValidateTokenRequest.parseFrom(new ByteArrayInputStream(original.toByteArray())))
                .isEqualTo(original);
    }

    @Test
    void parseDelimitedFromRoundTrips() throws Exception {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        original.writeDelimitedTo(out);

        assertThat(ValidateTokenRequest.parseDelimitedFrom(new ByteArrayInputStream(out.toByteArray())))
                .isEqualTo(original);
    }

    @Test
    void parseFromCodedInputStreamRoundTrips() throws Exception {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

        com.google.protobuf.CodedInputStream input =
                com.google.protobuf.CodedInputStream.newInstance(original.toByteArray());
        assertThat(ValidateTokenRequest.parseFrom(input)).isEqualTo(original);
    }

    @Test
    void writeToAndSerializedSizeMatch() {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

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
        assertThat(ValidateTokenRequest.getDescriptor().getName()).isEqualTo("ValidateTokenRequest");
        assertThat(ValidateTokenRequest.newBuilder().getDescriptorForType())
                .isEqualTo(ValidateTokenRequest.getDescriptor());
        assertThat(ValidateTokenRequest.newBuilder().getDefaultInstanceForType())
                .isEqualTo(ValidateTokenRequest.getDefaultInstance());
    }

    @Test
    void toStringContainsFieldValue() {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder().setAccessToken("token").build();

        assertThat(request.toString()).contains("token");
    }


    @Test
    void parseFromInputStreamWithRegistryRoundTrips() throws Exception {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        ValidateTokenRequest parsed = ValidateTokenRequest.parseFrom(
                new java.io.ByteArrayInputStream(original.toByteArray()),
                com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromByteStringRoundTrips() throws com.google.protobuf.InvalidProtocolBufferException {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        ValidateTokenRequest parsed = ValidateTokenRequest.parseFrom(com.google.protobuf.ByteString.copyFrom(original.toByteArray()));
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseDelimitedFromWithRegistryRoundTrips() throws Exception {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        original.writeDelimitedTo(out);
        ValidateTokenRequest parsed = ValidateTokenRequest.parseDelimitedFrom(
                new java.io.ByteArrayInputStream(out.toByteArray()),
                com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromParserRoundTrips() throws Exception {
        ValidateTokenRequest original = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        com.google.protobuf.Parser<ValidateTokenRequest> parser = ValidateTokenRequest.parser();
        ValidateTokenRequest parsed = parser.parseFrom(original.toByteArray());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void hashCodeVariesWithField() {
        ValidateTokenRequest first = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        ValidateTokenRequest second = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        ValidateTokenRequest empty = ValidateTokenRequest.getDefaultInstance();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(empty.hashCode()).isNotEqualTo(first.hashCode());
    }

    @Test
    void equalsHandlesMismatchedAndEmpty() {
        ValidateTokenRequest first = ValidateTokenRequest.newBuilder().setAccessToken("token").build();
        assertThat(first.equals(first)).isTrue();
        assertThat(first.equals(null)).isFalse();
        Object nonMessage = "not-a-message";
        assertThat(first.equals(nonMessage)).isFalse();
        assertThat(ValidateTokenRequest.getDefaultInstance().equals(ValidateTokenRequest.getDefaultInstance())).isTrue();
    }

    @Test
    void toStringHandlesEmptyInstance() {
        assertThat(ValidateTokenRequest.getDefaultInstance().toString()).isNotNull();
    }

    @Test
    void invalidParseThrows() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                ValidateTokenRequest.parseFrom(new byte[]{(byte) 0xff}))
                .isInstanceOf(com.google.protobuf.InvalidProtocolBufferException.class);
    }
}
