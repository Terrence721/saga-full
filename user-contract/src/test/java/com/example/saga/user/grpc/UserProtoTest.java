package com.example.saga.user.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProtoTest {

    @Test
    void getDescriptor_isNotNullAndNamedUserProto() {
        Descriptors.FileDescriptor descriptor = UserProto.getDescriptor();

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.getName()).isEqualTo("user.proto");
        assertThat(descriptor.getPackage()).isEqualTo("saga.user.v1");
        assertThat(descriptor.getMessageTypes()).hasSize(2);
    }

    @Test
    void registerAllExtensions_doesNotThrowForLiteRegistry() {
        ExtensionRegistryLite registry = ExtensionRegistryLite.getEmptyRegistry();

        UserProto.registerAllExtensions(registry);

        assertThat(registry).isNotNull();
    }

    @Test
    void registerAllExtensions_doesNotThrowForFullRegistry() {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();

        UserProto.registerAllExtensions(registry);

        assertThat(registry).isNotNull();
    }

    @Test
    void userProtoClass_canBeInstantiatedViaReflection() throws Exception {
        // Covers the private constructor path if present.
        var constructor = UserProto.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
