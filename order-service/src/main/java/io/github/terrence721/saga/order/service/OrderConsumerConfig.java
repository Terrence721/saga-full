package io.github.terrence721.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.order.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.order.exception.OrderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Component
@Slf4j
public class OrderConsumerConfig {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderConsumerConfig(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "restaurant-approved-topic", groupId = "order-approved-group")
    public void onRestaurantApproved(String payload) throws Exception {
        RestaurantApprovedEvent event = objectMapper.readValue(payload, RestaurantApprovedEvent.class);
        log.info("Received RestaurantApprovedEvent for order {}", event.orderId());
        orderService.confirmOrder(event);
    }

    @KafkaListener(topics = "restaurant-rejected-topic", groupId = "order-rejected-group")
    public void onRestaurantRejected(String payload) throws Exception {
        RestaurantRejectedEvent event = objectMapper.readValue(payload, RestaurantRejectedEvent.class);
        log.info("Received RestaurantRejectedEvent for order {}", event.orderId());
        orderService.cancelOrder(event);
    }

    /**
     * Without this bean, Spring Boot's autoconfigured default (10 retries, 0ms backoff, then
     * log-and-skip) silently takes over for anything either listener above throws - a
     * malformed payload or an event referencing an order this instance doesn't recognize.
     * Bounds the retries, skips them entirely for exceptions retrying can't fix - a missing
     * order ({@link OrderNotFoundException}) or event data that doesn't match what this
     * instance already has on record ({@code IllegalArgumentException}, raised by
     * {@link OrderService}'s own validation) - and replaces the default's recovery log with
     * one that actually names the topic/partition/offset.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Giving up on Kafka record: topic={} partition={} offset={} - {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage()),
                new FixedBackOff(1_000L, 2L));
        handler.addNotRetryableExceptions(OrderNotFoundException.class, IllegalArgumentException.class);
        return handler;
    }
}
