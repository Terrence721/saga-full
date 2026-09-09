package io.github.terrence721.saga.payment.service;

import io.github.terrence721.saga.payment.exception.PaymentNotFoundException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class KafkaErrorHandlerConfigTest {

    private final DefaultErrorHandler handler = new KafkaErrorHandlerConfig().kafkaErrorHandler();

    @Test
    void kafkaErrorHandler_recoversImmediatelyForPaymentNotFoundException() {
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("restaurant-rejected-topic", 0, 5L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean handled = handler.handleOne(
                new PaymentNotFoundException("Payment not found for order: x"), record, consumer, container);

        assertThat(handled).isTrue();
    }

    @Test
    void kafkaErrorHandler_recoversImmediatelyForIllegalArgumentException() {
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("restaurant-rejected-topic", 0, 9L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean handled = handler.handleOne(
                new IllegalArgumentException("customerId mismatch for order x"), record, consumer, container);

        assertThat(handled).isTrue();
    }

    @Test
    void kafkaErrorHandler_retriesGenericExceptionsBeforeGivingUp() {
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>("order-created-topic", 0, 1L, "key", "payload");
        Consumer<?, ?> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        boolean firstAttempt = handler.handleOne(new IllegalStateException("boom"), record, consumer, container);

        assertThat(firstAttempt).isFalse();
    }
}
