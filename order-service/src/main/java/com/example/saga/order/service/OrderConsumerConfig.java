package com.example.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.order.dto.ReservationConfirmedEvent;
import com.example.saga.order.dto.ReservationRejectedEvent;
import com.example.saga.order.dto.RestaurantApprovedEvent;
import com.example.saga.order.dto.RestaurantRejectedEvent;
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
        log.info("Received RestaurantApprovedEvent for order {} (awaiting reservation confirmation)", event.orderId());
    }

    @KafkaListener(topics = "restaurant-rejected-topic", groupId = "order-rejected-group")
    public void onRestaurantRejected(String payload) throws Exception {
        RestaurantRejectedEvent event = objectMapper.readValue(payload, RestaurantRejectedEvent.class);
        log.info("Received RestaurantRejectedEvent for order {}", event.orderId());
        orderService.cancelOrder(event);
    }

    @KafkaListener(topics = "reservation-confirmed-topic", groupId = "order-reservation-confirmed-group")
    public void onReservationConfirmed(String payload) throws Exception {
        ReservationConfirmedEvent event = objectMapper.readValue(payload, ReservationConfirmedEvent.class);
        log.info("Received ReservationConfirmedEvent for order {}", event.orderId());
        orderService.handleReservationConfirmed(event);
    }

    @KafkaListener(topics = "reservation-rejected-topic", groupId = "order-reservation-rejected-group")
    public void onReservationRejected(String payload) throws Exception {
        ReservationRejectedEvent event = objectMapper.readValue(payload, ReservationRejectedEvent.class);
        log.info("Received ReservationRejectedEvent for order {}", event.orderId());
        orderService.handleReservationRejected(event);
    }
}
