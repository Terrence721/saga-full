package com.example.saga.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.saga.payment.dto.OrderCreatedEvent;
import com.example.saga.payment.dto.OrderStatus;
import com.example.saga.payment.dto.RestaurantRejectedEvent;
import com.example.saga.payment.dto.ReservationRejectedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerConfigTest {

    @Mock
    private PaymentService paymentService;

    private PaymentConsumerConfig consumerConfig;

    @BeforeEach
    void setUp() {
        consumerConfig = new PaymentConsumerConfig(paymentService, new ObjectMapper());
    }

    @Test
    void onOrderCreated_deserializesPayloadAndDelegatesToProcessPaymentSaga() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new OrderCreatedEvent(orderId, customerId, "ITEM-1", 2, new BigDecimal("19.99"), OrderStatus.PENDING));

        consumerConfig.onOrderCreated(payload);

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(paymentService).processPaymentSaga(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().itemCode()).isEqualTo("ITEM-1");
        assertThat(captor.getValue().quantity()).isEqualTo(2);
        assertThat(captor.getValue().totalAmount()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(captor.getValue().status()).isEqualTo(OrderStatus.PENDING);

        verify(paymentService, never()).handleOrderCompensation(any());
        verify(paymentService, never()).handleReservationCompensation(any());
    }

    @Test
    void onOrderCreated_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onOrderCreated("{not json}"))
                .isInstanceOf(IOException.class);
        verify(paymentService, never()).processPaymentSaga(any());
    }

    @Test
    void onOrderCreated_rejectsNullPayload() {
        assertThatThrownBy(() -> consumerConfig.onOrderCreated(null))
                .isInstanceOf(Exception.class);
        verify(paymentService, never()).processPaymentSaga(any());
    }

    @Test
    void onRestaurantRejected_deserializesPayloadAndDelegatesToHandleOrderCompensation() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantRejectedEvent(orderId, customerId, "Out of stock"));

        consumerConfig.onRestaurantRejected(payload);

        ArgumentCaptor<RestaurantRejectedEvent> captor = ArgumentCaptor.forClass(RestaurantRejectedEvent.class);
        verify(paymentService).handleOrderCompensation(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().reason()).isEqualTo("Out of stock");

        verify(paymentService, never()).processPaymentSaga(any());
        verify(paymentService, never()).handleReservationCompensation(any());
    }

    @Test
    void onRestaurantRejected_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onRestaurantRejected("{not json}"))
                .isInstanceOf(IOException.class);
        verify(paymentService, never()).handleOrderCompensation(any());
    }

    @Test
    void onReservationRejected_deserializesPayloadAndDelegatesToHandleReservationCompensation() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new ReservationRejectedEvent(orderId, customerId, "no table"));

        consumerConfig.onReservationRejected(payload);

        ArgumentCaptor<ReservationRejectedEvent> captor = ArgumentCaptor.forClass(ReservationRejectedEvent.class);
        verify(paymentService).handleReservationCompensation(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().reason()).isEqualTo("no table");

        verify(paymentService, never()).processPaymentSaga(any());
        verify(paymentService, never()).handleOrderCompensation(any());
    }

    @Test
    void onReservationRejected_rejectsMalformedJson() {
        assertThatThrownBy(() -> consumerConfig.onReservationRejected("{not json}"))
                .isInstanceOf(IOException.class);
        verify(paymentService, never()).handleReservationCompensation(any());
    }

    @Test
    void onOrderCreated_isBoundToOrderCreatedTopic() throws NoSuchMethodException {
        KafkaListener annotation = PaymentConsumerConfig.class
                .getMethod("onOrderCreated", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("order-created-topic");
        assertThat(annotation.groupId()).isEqualTo("payment-group");
    }

    @Test
    void onRestaurantRejected_isBoundToRestaurantRejectedTopic() throws NoSuchMethodException {
        KafkaListener annotation = PaymentConsumerConfig.class
                .getMethod("onRestaurantRejected", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("restaurant-rejected-topic");
        assertThat(annotation.groupId()).isEqualTo("payment-compensation-group");
    }

    @Test
    void onReservationRejected_isBoundToReservationRejectedTopic() throws NoSuchMethodException {
        KafkaListener annotation = PaymentConsumerConfig.class
                .getMethod("onReservationRejected", String.class)
                .getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topics()).containsExactly("reservation-rejected-topic");
        assertThat(annotation.groupId()).isEqualTo("payment-reservation-compensation-group");
    }
}
