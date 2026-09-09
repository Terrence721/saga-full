package io.github.terrence721.saga.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.payment.dto.OrderCreatedEvent;
import io.github.terrence721.saga.payment.dto.RestaurantRejectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
}
