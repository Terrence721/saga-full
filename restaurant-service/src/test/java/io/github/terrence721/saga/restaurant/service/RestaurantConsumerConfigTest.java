package io.github.terrence721.saga.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.restaurant.dto.PaymentProcessedEvent;
import io.github.terrence721.saga.restaurant.dto.PaymentStatus;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class RestaurantConsumerConfigTest {

    @Mock
    private RestaurantService restaurantService;

    private RestaurantConsumerConfig consumerConfig;

    @BeforeEach
    void setUp() {
        consumerConfig = new RestaurantConsumerConfig(restaurantService, new ObjectMapper());
    }

    @Test
    void onPaymentProcessed_deserializesPayloadAndDelegatesToProcessRestaurantStep() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String payload = new ObjectMapper().writeValueAsString(
                new PaymentProcessedEvent(orderId, customerId, "PIZZA_01", 2, new BigDecimal("19.99"), PaymentStatus.APPROVED));

        consumerConfig.onPaymentProcessed(payload);

        ArgumentCaptor<PaymentProcessedEvent> captor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
        verify(restaurantService).processRestaurantStep(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(new BigDecimal("19.99"));
    }

    @Test
    void kafkaErrorHandler_recoversImmediatelyForIllegalArgumentException() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("payment-processed-topic", 0, 9L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean handled = handler.handleOne(
                new IllegalArgumentException("itemCode is required"), record, consumer, container);

        assertThat(handled).isTrue();
    }

    @Test
    void kafkaErrorHandler_retriesGenericExceptionsBeforeGivingUp() {
        DefaultErrorHandler handler = consumerConfig.kafkaErrorHandler();
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("payment-processed-topic", 0, 1L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean firstAttempt = handler.handleOne(new IllegalStateException("boom"), record, consumer, container);

        assertThat(firstAttempt).isFalse();
    }
}
