package com.example.saga.reservation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    /**
     * Bounds retries and makes permanently-bad records non-retryable, matching the
     * error handling already established in the other saga services.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on Kafka record: topic={} partition={} offset={} - {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage()),
                new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
