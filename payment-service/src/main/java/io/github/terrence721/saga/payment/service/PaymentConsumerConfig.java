package io.github.terrence721.saga.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.payment.dto.OrderCreatedEvent;
import io.github.terrence721.saga.payment.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.payment.exception.PaymentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Component
@Slf4j
public class PaymentConsumerConfig {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentConsumerConfig(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-created-topic", groupId = "payment-group")
    public void onOrderCreated(String payload) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        log.info("Received OrderCreatedEvent for order {}", event.orderId());
        paymentService.processPaymentSaga(event);
    }

    @KafkaListener(topics = "restaurant-rejected-topic", groupId = "payment-compensation-group")
    public void onRestaurantRejected(String payload) throws Exception {
        RestaurantRejectedEvent event = objectMapper.readValue(payload, RestaurantRejectedEvent.class);
        log.info("Received RestaurantRejectedEvent for order {}", event.orderId());
        paymentService.handleOrderCompensation(event);
    }

    /**
     * Without this bean, Spring Boot's autoconfigured default (10 retries, 0ms backoff, then
     * log-and-skip) silently takes over for anything either listener above throws - a
     * malformed payload or an event referencing an order this instance doesn't recognize.
     * Bounds the retries, skips them entirely for exceptions retrying can't fix - a missing
     * payment ({@link PaymentNotFoundException}) or event data that doesn't match what this
     * instance already has on record ({@code IllegalArgumentException}, raised by
     * {@link PaymentService}'s own validation) - and replaces the default's recovery log with
     * one that actually names the topic/partition/offset.
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
