package io.github.terrence721.saga.payment.service;

import io.github.terrence721.saga.payment.exception.PaymentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    /**
     * Without this bean, Spring Boot's autoconfigured default (10 retries, 0ms backoff, then
     * log-and-skip) silently takes over for anything a {@code @KafkaListener} method throws
     * (see {@link PaymentConsumerConfig}) - a malformed payload or an event referencing a
     * payment this instance doesn't recognize. Bounds the retries, skips them entirely for
     * exceptions retrying can't fix - a missing payment ({@link PaymentNotFoundException}) or
     * event data that doesn't match what this instance already has on record
     * ({@code IllegalArgumentException}, raised by {@link PaymentService}'s own validation) -
     * and replaces the default's recovery log with one that actually names the
     * topic/partition/offset.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on Kafka record: topic={} partition={} offset={} - {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage()),
                new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(PaymentNotFoundException.class, IllegalArgumentException.class);
        return handler;
    }
}
