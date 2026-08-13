package io.github.terrence721.saga.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc;
import io.github.terrence721.saga.user.grpc.UserIdentityServiceGrpc.UserIdentityServiceBlockingStub;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CommonAppConfig {

    /**
     * Matches config in application.yaml under spring.grpc.client.channels.userService.
     */
    @Bean
    UserIdentityServiceBlockingStub userIdentityServiceStub(GrpcChannelFactory channelFactory) {
        log.info("Auto-configuring UserIdentityServiceBlockingStub using named channel: [userService]");
        return UserIdentityServiceGrpc.newBlockingStub(
                channelFactory.createChannel("userService")
        );
    }

}
