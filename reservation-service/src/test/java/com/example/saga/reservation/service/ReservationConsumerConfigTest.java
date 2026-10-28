package com.example.saga.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.reservation.dto.RestaurantApprovedEvent;
import com.example.saga.reservation.dto.RestaurantRejectedEvent;
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
class ReservationConsumerConfigTest {

    @Mock
    private ReservationService reservationService;

    private ReservationConsumerConfig consumerConfig;

    @BeforeEach
    void setUp() {
        consumerConfig = new ReservationConsumerConfig(reservationService, new ObjectMapper());
    }

    @Test
    void onRestaurantApproved_deserializesPayloadAndDelegates() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantApprovedEvent(orderId, customerId, ticketId));

        consumerConfig.onRestaurantApproved(payload);

        ArgumentCaptor<RestaurantApprovedEvent> captor = ArgumentCaptor.forClass(RestaurantApprovedEvent.class);
        verify(reservationService).handleRestaurantApproved(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
    }

    @Test
    void onRestaurantRejected_deserializesPayloadAndDelegates() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantRejectedEvent(orderId, customerId, "Out of stock"));

        consumerConfig.onRestaurantRejected(payload);

        ArgumentCaptor<RestaurantRejectedEvent> captor = ArgumentCaptor.forClass(RestaurantRejectedEvent.class);
        verify(reservationService).handleRestaurantRejected(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().reason()).isEqualTo("Out of stock");
    }

    @Test
    void onRestaurantApproved_doesNotCallRejectedPath() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantApprovedEvent(orderId, customerId, UUID.randomUUID()));

        consumerConfig.onRestaurantApproved(payload);

        verify(reservationService, never()).handleRestaurantRejected(any());
    }
}
