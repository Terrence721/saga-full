package io.github.terrence721.saga.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.restaurant.dto.PaymentProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
}
