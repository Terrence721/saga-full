package io.github.terrence721.saga.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.grpc.client.GrpcChannelFactory;

import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc.UserIdentityServiceBlockingStub;
import io.grpc.ManagedChannel;

class CommonAppConfigTest {

    /**
     * "userService" is a plain string literal that must exactly match
     * application.yaml's spring.grpc.client.channels.userService key - nothing
     * else in this repo verifies that coupling, since every other test mocks
     * UserGrpcClient/its stub away entirely rather than exercising this bean's
     * real wiring.
     */
    @Test
    void userIdentityServiceStub_CreatesChannel_UsingTheUserServiceNameFromApplicationYaml() {
        GrpcChannelFactory channelFactory = mock(GrpcChannelFactory.class);
        ManagedChannel channel = mock(ManagedChannel.class);
        when(channelFactory.createChannel("userService")).thenReturn(channel);

        UserIdentityServiceBlockingStub stub = new CommonAppConfig().userIdentityServiceStub(channelFactory);

        verify(channelFactory).createChannel("userService");
        assertThat(stub.getChannel()).isSameAs(channel);
    }
}
