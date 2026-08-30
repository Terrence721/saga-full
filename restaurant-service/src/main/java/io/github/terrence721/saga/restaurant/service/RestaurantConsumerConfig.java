package io.github.terrence721.saga.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.restaurant.dto.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Component
@Slf4j
public class RestaurantConsumerConfig {

    private final RestaurantService restaurantService;
    private final ObjectMapper objectMapper;

    public RestaurantConsumerConfig(RestaurantService restaurantService, ObjectMapper objectMapper) {
        this.restaurantService = restaurantService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-processed-topic", groupId = "restaurant-group")
    public void onPaymentProcessed(String payload) throws Exception {
        PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
        log.info("Received PaymentProcessedEvent for order {}", event.orderId());
        restaurantService.processRestaurantStep(event);
    }

    /**
     * Without this bean, Spring Boot's autoconfigured default (10 retries, 0ms backoff, then
     * log-and-skip) silently takes over for anything the listener above throws - a malformed
     * payload or an event with fields {@link RestaurantService}'s own validation rejects.
     * Bounds the retries, skips them entirely for {@code IllegalArgumentException} (retrying
     * can't fix a permanently malformed message), and replaces the default's recovery log with
     * one that actually names the topic/partition/offset.
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
