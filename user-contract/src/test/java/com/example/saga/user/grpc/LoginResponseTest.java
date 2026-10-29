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


    @Test
    void parseFromInputStreamWithRegistryRoundTrips() throws Exception {
        LoginResponse original = LoginResponse.newBuilder()
                .setUserId("u")
                .setAccessToken("tok")
                .setTokenType("Bearer")
                .setExpiresInSeconds(3600L)
                .build();
        LoginResponse parsed = LoginResponse.parseFrom(
                new java.io.ByteArrayInputStream(original.toByteArray()),
                com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromByteStringRoundTrips() throws com.google.protobuf.InvalidProtocolBufferException {
        LoginResponse original = LoginResponse.newBuilder().setUserId("u").build();
        LoginResponse parsed = LoginResponse.parseFrom(com.google.protobuf.ByteString.copyFrom(original.toByteArray()));
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseDelimitedFromWithRegistryRoundTrips() throws Exception {
        LoginResponse original = LoginResponse.newBuilder().setUserId("u").build();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        original.writeDelimitedTo(out);
        LoginResponse parsed = LoginResponse.parseDelimitedFrom(
                new java.io.ByteArrayInputStream(out.toByteArray()),
                com.google.protobuf.ExtensionRegistryLite.getEmptyRegistry());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void parseFromParserRoundTrips() throws Exception {
        LoginResponse original = LoginResponse.newBuilder().setUserId("u").build();
        com.google.protobuf.Parser<LoginResponse> parser = LoginResponse.parser();
        LoginResponse parsed = parser.parseFrom(original.toByteArray());
        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void hashCodeVariesWithFields() {
        LoginResponse first = LoginResponse.newBuilder().setUserId("u").setExpiresInSeconds(1L).build();
        LoginResponse second = LoginResponse.newBuilder().setUserId("u").setExpiresInSeconds(1L).build();
        LoginResponse empty = LoginResponse.getDefaultInstance();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(empty.hashCode()).isNotEqualTo(first.hashCode());
    }

    @Test
    void equalsHandlesMismatchedAndEmpty() {
        LoginResponse first = LoginResponse.newBuilder().setUserId("u").setExpiresInSeconds(1L).build();
        assertThat(first.equals(first)).isTrue();
        assertThat(first.equals(null)).isFalse();
        assertThat(first.equals("not-a-message")).isFalse();
        assertThat(LoginResponse.getDefaultInstance().equals(LoginResponse.getDefaultInstance())).isTrue();
    }

    @Test
    void toStringHandlesEmptyInstance() {
        assertThat(LoginResponse.getDefaultInstance().toString()).isNotNull();
    }

    @Test
    void invalidParseThrows() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                LoginResponse.parseFrom(new byte[]{(byte) 0xff}))
                .isInstanceOf(com.google.protobuf.InvalidProtocolBufferException.class);
    }


    @Test
    void unknownFieldsArePreserved() {
        com.google.protobuf.UnknownFieldSet unknown =
                com.google.protobuf.UnknownFieldSet.newBuilder()
                        .addField(999, com.google.protobuf.UnknownFieldSet.Field.newBuilder().addFixed32(7).build())
                        .build();
        LoginResponse withUnknown = LoginResponse.newBuilder()
                .setUserId("u")
                .setUnknownFields(unknown)
                .build();
        assertThat(withUnknown.getUnknownFields()).isEqualTo(unknown);
        assertThat(withUnknown.toString()).contains("999");
    }

    @Test
    void mergeFromMergesUnknownFields() {
        com.google.protobuf.UnknownFieldSet unknown =
                com.google.protobuf.UnknownFieldSet.newBuilder()
                        .addField(999, com.google.protobuf.UnknownFieldSet.Field.newBuilder().addFixed32(7).build())
                        .build();
        LoginResponse merged = LoginResponse.newBuilder()
                .setUserId("old")
                .mergeFrom(LoginResponse.newBuilder().setUnknownFields(unknown).build())
                .build();
        assertThat(merged.getUnknownFields()).isEqualTo(unknown);
    }

    @Test
    void equalsConsidersUnknownFields() {
        com.google.protobuf.UnknownFieldSet unknown =
                com.google.protobuf.UnknownFieldSet.newBuilder()
                        .addField(999, com.google.protobuf.UnknownFieldSet.Field.newBuilder().addFixed32(7).build())
                        .build();
        LoginResponse first = LoginResponse.newBuilder().setUserId("u").setUnknownFields(unknown).build();
        LoginResponse second = LoginResponse.newBuilder().setUserId("u").setUnknownFields(unknown).build();
        LoginResponse none = LoginResponse.newBuilder().setUserId("u").build();
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(none);
    }

    @Test
    void parseUnknownFieldRoundTrips() throws Exception {
        com.google.protobuf.UnknownFieldSet unknown =
                com.google.protobuf.UnknownFieldSet.newBuilder()
                        .addField(999, com.google.protobuf.UnknownFieldSet.Field.newBuilder().addFixed32(7).build())
                        .build();
        LoginResponse original = LoginResponse.newBuilder()
                .setUserId("u")
                .setUnknownFields(unknown)
                .build();
        LoginResponse parsed = LoginResponse.parseFrom(original.toByteArray());
        assertThat(parsed.getUnknownFields()).isEqualTo(unknown);
    }
}
