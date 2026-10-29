package com.example.saga.restaurant.integration;

import com.example.saga.restaurant.domain.RestaurantTicketStatus;
import com.example.saga.restaurant.dto.PaymentProcessedEvent;
import com.example.saga.restaurant.dto.PaymentStatus;
import com.example.saga.restaurant.repository.RestaurantTicketRepository;
import com.example.saga.restaurant.repository.OutboxRepository;
import com.example.saga.restaurant.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RestaurantSagaIntegrationTest {

    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private RestaurantTicketRepository ticketRepository;
    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @Transactional
    void approvedPathCreatesPreparingTicketAndApprovedOutbox() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        PaymentProcessedEvent event = new PaymentProcessedEvent(orderId, customerId, "MARGHERITA", 1, new BigDecimal("8.50"), PaymentStatus.APPROVED);

        restaurantService.processRestaurantStep(event);

        assertThat(ticketRepository.findAll().stream()
            .filter(t -> t.getOrderId().equals(orderId))
            .map(ticket -> ticket.getStatus())
            .findFirst())
            .contains(RestaurantTicketStatus.PREPARING);
        assertThat(outboxRepository.findAll()).anySatisfy(r -> assertThat(r.getEventType()).isEqualTo("RestaurantApprovedEvent"));
    }

    @Test
    @Transactional
    void insufficientStockCreatesRejectedTicketAndRejectedOutbox() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        PaymentProcessedEvent event = new PaymentProcessedEvent(orderId, customerId, "TRUFFLE", 2, new BigDecimal("28.00"), PaymentStatus.APPROVED);

        restaurantService.processRestaurantStep(event);

        assertThat(ticketRepository.findAll().stream()
            .filter(t -> t.getOrderId().equals(orderId))
            .map(ticket -> ticket.getStatus())
            .findFirst())
            .contains(RestaurantTicketStatus.REJECTED);
        assertThat(outboxRepository.findAll()).anySatisfy(r -> assertThat(r.getEventType()).isEqualTo("RestaurantRejectedEvent"));
    }
}