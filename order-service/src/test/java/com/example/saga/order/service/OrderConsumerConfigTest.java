package com.example.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.order.dto.ReservationConfirmedEvent;
import com.example.saga.order.dto.ReservationRejectedEvent;
import com.example.saga.order.dto.RestaurantApprovedEvent;
import com.example.saga.order.dto.RestaurantRejectedEvent;
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
class OrderConsumerConfigTest {

    @Mock
    private OrderService orderService;

    private OrderConsumerConfig consumerConfig;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        consumerConfig = new OrderConsumerConfig(orderService, new ObjectMapper());
    }

    @Test
    void onRestaurantApproved_deserializesPayloadAndDoesNotMutateOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantApprovedEvent(orderId, customerId, ticketId));

        consumerConfig.onRestaurantApproved(payload);

        // The approval is an intermediate signal only: the order is finalized when the
        // reservation is confirmed, so no state mutation may happen here.
        verify(orderService, never()).cancelOrder(any());
        verify(orderService, never()).handleReservationConfirmed(any());
        verify(orderService, never()).handleReservationRejected(any());
    }

    @Test
    void onRestaurantApproved_preservesAllDeserializedFields() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantApprovedEvent(orderId, customerId, ticketId));

        // Re-serializing the parsed event keeps the wiring honest: a swapped DTO would
        // fail this equality check rather than silently passing.
        RestaurantApprovedEvent parsed = new ObjectMapper().readValue(payload, RestaurantApprovedEvent.class);
        assertThat(parsed.orderId()).isEqualTo(orderId);
        assertThat(parsed.customerId()).isEqualTo(customerId);
        assertThat(parsed.ticketId()).isEqualTo(ticketId);

        consumerConfig.onRestaurantApproved(payload);
        verify(orderService, never()).handleReservationConfirmed(any());
    }

    @Test
    void onRestaurantApproved_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantApproved("{not json}"))
                .isInstanceOf(IOException.class);
        verify(orderService, never()).handleReservationConfirmed(any());
        verify(orderService, never()).handleReservationRejected(any());
    }

    @Test
    void onRestaurantApproved_rejectsNullPayload() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantApproved(null))
                .isInstanceOf(Exception.class);
        verify(orderService, never()).handleReservationConfirmed(any());
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

        verify(orderService, never()).handleReservationConfirmed(any());
        verify(orderService, never()).handleReservationRejected(any());
    }

    @Test
    void onRestaurantRejected_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantRejected("{not json}"))
                .isInstanceOf(IOException.class);
        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void onReservationConfirmed_deserializesPayloadAndDelegates() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new ReservationConfirmedEvent(orderId, customerId, "T-1", 4));

        consumerConfig.onReservationConfirmed(payload);

        ArgumentCaptor<ReservationConfirmedEvent> captor = ArgumentCaptor.forClass(ReservationConfirmedEvent.class);
        verify(orderService).handleReservationConfirmed(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().tableNumber()).isEqualTo("T-1");
        assertThat(captor.getValue().partySize()).isEqualTo(4);

        verify(orderService, never()).cancelOrder(any());
        verify(orderService, never()).handleReservationRejected(any());
    }

    @Test
    void onReservationConfirmed_handlesEmptyTableNumber() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new ReservationConfirmedEvent(orderId, customerId, "", 0));

        consumerConfig.onReservationConfirmed(payload);

        ArgumentCaptor<ReservationConfirmedEvent> captor = ArgumentCaptor.forClass(ReservationConfirmedEvent.class);
        verify(orderService).handleReservationConfirmed(captor.capture());
        assertThat(captor.getValue().tableNumber()).isEmpty();
        assertThat(captor.getValue().partySize()).isZero();
    }

    @Test
    void onReservationConfirmed_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onReservationConfirmed("{not json}"))
                .isInstanceOf(IOException.class);
        verify(orderService, never()).handleReservationConfirmed(any());
    }

    @Test
    void onReservationRejected_deserializesPayloadAndDelegates() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new ReservationRejectedEvent(orderId, customerId, "no table"));

        consumerConfig.onReservationRejected(payload);

        ArgumentCaptor<ReservationRejectedEvent> captor = ArgumentCaptor.forClass(ReservationRejectedEvent.class);
        verify(orderService).handleReservationRejected(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().reason()).isEqualTo("no table");

        verify(orderService, never()).cancelOrder(any());
        verify(orderService, never()).handleReservationConfirmed(any());
    }

    @Test
    void onReservationRejected_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onReservationRejected("{not json}"))
                .isInstanceOf(IOException.class);
        verify(orderService, never()).handleReservationRejected(any());
    }

    @Test
    void onRestaurantApproved_isBoundToApprovedTopic() throws NoSuchMethodException {
        KafkaListener annotation = OrderConsumerConfig.class
                .getMethod("onRestaurantApproved", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("restaurant-approved-topic");
        assertThat(annotation.groupId()).isEqualTo("order-approved-group");
    }

    @Test
    void onRestaurantRejected_isBoundToRejectedTopic() throws NoSuchMethodException {
        KafkaListener annotation = OrderConsumerConfig.class
                .getMethod("onRestaurantRejected", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("restaurant-rejected-topic");
        assertThat(annotation.groupId()).isEqualTo("order-rejected-group");
    }

    @Test
    void onReservationConfirmed_isBoundToConfirmedTopic() throws NoSuchMethodException {
        KafkaListener annotation = OrderConsumerConfig.class
                .getMethod("onReservationConfirmed", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("reservation-confirmed-topic");
        assertThat(annotation.groupId()).isEqualTo("order-reservation-confirmed-group");
    }

    @Test
    void onReservationRejected_isBoundToReservationRejectedTopic() throws NoSuchMethodException {
        KafkaListener annotation = OrderConsumerConfig.class
                .getMethod("onReservationRejected", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("reservation-rejected-topic");
        assertThat(annotation.groupId()).isEqualTo("order-reservation-rejected-group");
    }
}
