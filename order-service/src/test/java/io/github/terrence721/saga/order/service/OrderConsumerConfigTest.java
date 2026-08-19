package io.github.terrence721.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.dto.RestaurantApprovedEvent;
import io.github.terrence721.saga.order.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.order.exception.OrderNotFoundException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    void onRestaurantApproved_deserializesPayloadAndDelegatesToConfirmOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new RestaurantApprovedEvent(orderId, customerId, ticketId));

        consumerConfig.onRestaurantApproved(payload);

        ArgumentCaptor<RestaurantApprovedEvent> captor = ArgumentCaptor.forClass(RestaurantApprovedEvent.class);
        verify(orderService).confirmOrder(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().ticketId()).isEqualTo(ticketId);
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

    @Test
    void kafkaErrorHandler_recoversImmediatelyForOrderNotFoundException() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("restaurant-rejected-topic", 0, 5L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean handled = handler.handleOne(new OrderNotFoundException("Order not found: x"), record, consumer, container);

        assertThat(handled).isTrue();
    }

    @Test
    void kafkaErrorHandler_retriesGenericExceptionsBeforeGivingUp() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("restaurant-approved-topic", 0, 1L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean firstAttempt = handler.handleOne(new IllegalStateException("boom"), record, consumer, container);

        assertThat(firstAttempt).isFalse();
    }
}
