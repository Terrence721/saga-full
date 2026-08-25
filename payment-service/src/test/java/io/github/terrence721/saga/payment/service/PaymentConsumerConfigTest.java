package io.github.terrence721.saga.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.payment.dto.OrderCreatedEvent;
import io.github.terrence721.saga.payment.dto.OrderStatus;
import io.github.terrence721.saga.payment.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.payment.exception.PaymentNotFoundException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        assertThat(captor.getValue().totalAmount()).isEqualByComparingTo(new BigDecimal("19.99"));
        verify(paymentService, never()).handleOrderCompensation(any());
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
    }

    @Test
    void kafkaErrorHandler_recoversImmediatelyForPaymentNotFoundException() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("restaurant-rejected-topic", 0, 5L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean handled = handler.handleOne(
                new PaymentNotFoundException("Payment not found for order: x"), record, consumer, container);

        assertThat(handled).isTrue();
    }

    @Test
    void kafkaErrorHandler_recoversImmediatelyForIllegalArgumentException() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("restaurant-rejected-topic", 0, 9L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean handled = handler.handleOne(
                new IllegalArgumentException("customerId mismatch for order x"), record, consumer, container);

        assertThat(handled).isTrue();
    }

    @Test
    void kafkaErrorHandler_retriesGenericExceptionsBeforeGivingUp() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("order-created-topic", 0, 1L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean firstAttempt = handler.handleOne(new IllegalStateException("boom"), record, consumer, container);

        assertThat(firstAttempt).isFalse();
    }
}
