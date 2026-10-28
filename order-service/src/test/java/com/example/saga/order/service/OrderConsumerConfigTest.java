package com.example.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.order.dto.RestaurantApprovedEvent;
import com.example.saga.order.dto.RestaurantRejectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderConsumerConfigTest {

    @Mock
    private OrderService orderService;

    private OrderConsumerConfig consumerConfig;

    @BeforeEach
    void setUp() {
        consumerConfig = new OrderConsumerConfig(orderService, new ObjectMapper());
    }

    @Test
    void onRestaurantApproved_deserializesPayloadWithoutMutatingOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantApprovedEvent(orderId, customerId, ticketId));

        consumerConfig.onRestaurantApproved(payload);

        // Order finalization now waits for reservation confirmation instead.
        verify(orderService, never()).confirmOrder(any());
        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void onRestaurantRejected_deserializesPayloadAndDelegatesToCancelOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantRejectedEvent(orderId, customerId, "Out of stock"));

        consumerConfig.onRestaurantRejected(payload);

        ArgumentCaptor<RestaurantRejectedEvent> captor = ArgumentCaptor.forClass(RestaurantRejectedEvent.class);
        verify(orderService).cancelOrder(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().reason()).isEqualTo("Out of stock");
        verify(orderService, never()).confirmOrder(any());
    }
}
