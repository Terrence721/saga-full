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
import org.springframework.kafka.annotation.KafkaListener;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThat(captor.getValue().ticketId()).isEqualTo(ticketId);

        verify(reservationService, never()).handleRestaurantRejected(any());
    }

    @Test
    void onRestaurantApproved_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantApproved("{not json}"))
                .isInstanceOf(IOException.class);
        verify(reservationService, never()).handleRestaurantApproved(any());
    }

    @Test
    void onRestaurantApproved_rejectsNullPayload() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantApproved(null))
                .isInstanceOf(Exception.class);
        verify(reservationService, never()).handleRestaurantApproved(any());
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
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().reason()).isEqualTo("Out of stock");

        verify(reservationService, never()).handleRestaurantApproved(any());
    }

    @Test
    void onRestaurantRejected_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantRejected("{not json}"))
                .isInstanceOf(IOException.class);
        verify(reservationService, never()).handleRestaurantRejected(any());
    }

    @Test
    void onRestaurantApproved_isBoundToApprovedTopic() throws NoSuchMethodException {
        KafkaListener annotation = ReservationConsumerConfig.class
                .getMethod("onRestaurantApproved", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("restaurant-approved-topic");
        assertThat(annotation.groupId()).isEqualTo("reservation-approved-group");
    }

    @Test
    void onRestaurantRejected_isBoundToRejectedTopic() throws NoSuchMethodException {
        KafkaListener annotation = ReservationConsumerConfig.class
                .getMethod("onRestaurantRejected", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("restaurant-rejected-topic");
        assertThat(annotation.groupId()).isEqualTo("reservation-rejected-group");
    }
}
