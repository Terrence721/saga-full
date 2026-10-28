package com.example.saga.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.reservation.dto.RestaurantApprovedEvent;
import com.example.saga.reservation.dto.RestaurantRejectedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReservationConsumerConfig {

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    public ReservationConsumerConfig(ReservationService reservationService, ObjectMapper objectMapper) {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "restaurant-approved-topic", groupId = "reservation-approved-group")
    public void onRestaurantApproved(String payload) throws Exception {
        RestaurantApprovedEvent event = objectMapper.readValue(payload, RestaurantApprovedEvent.class);
        log.info("Received RestaurantApprovedEvent for order {}", event.orderId());
        reservationService.handleRestaurantApproved(event);
    }

    @KafkaListener(topics = "restaurant-rejected-topic", groupId = "reservation-rejected-group")
    public void onRestaurantRejected(String payload) throws Exception {
        RestaurantRejectedEvent event = objectMapper.readValue(payload, RestaurantRejectedEvent.class);
        log.info("Received RestaurantRejectedEvent for order {}", event.orderId());
        reservationService.handleRestaurantRejected(event);
    }
}
