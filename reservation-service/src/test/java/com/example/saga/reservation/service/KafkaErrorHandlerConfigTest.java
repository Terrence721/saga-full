package com.example.saga.reservation.service;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class KafkaErrorHandlerConfigTest {

    @Test
    void kafkaErrorHandler_recoversFailedRecord() {
        KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();
        DefaultErrorHandler handler = config.kafkaErrorHandler();
        assertThat(handler).isNotNull();

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "key", "value");
        Consumer<String, String> consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);

        assertThatCode(() -> handler.handleOne(
                new IllegalArgumentException("bad payload"), record, consumer, container))
                .doesNotThrowAnyException();
    }
}
