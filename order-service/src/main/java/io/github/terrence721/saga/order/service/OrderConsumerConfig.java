package io.github.terrence721.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.order.dto.RestaurantRejectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
}
