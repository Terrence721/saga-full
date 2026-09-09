package io.github.terrence721.saga.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.order.domain.OutboxRecord;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRecordFactoryTest {

    private final OutboxRecordFactory factory = new OutboxRecordFactory(new ObjectMapper());

    private record SampleEvent(String value) {
    }

    // Jackson tries to serialize this via getValue(), which throws - the same failure
    // shape a real event's getter throwing would hit, without depending on any real DTO.
    private static class Unserializable {
        @SuppressWarnings("unused") // invoked reflectively by Jackson, not called directly.
        public String getValue() {
            throw new RuntimeException("boom");
        }
    }

    @Test
    void create_buildsOutboxRecordWithSerializedPayload() {
        String aggregateId = UUID.randomUUID().toString();

        OutboxRecord record = factory.create(aggregateId, "SampleEvent", new SampleEvent("hello"));

        assertThat(record.getAggregateId()).isEqualTo(aggregateId);
        assertThat(record.getEventType()).isEqualTo("SampleEvent");
        assertThat(record.getPayload()).isEqualTo("{\"value\":\"hello\"}");
        assertThat(record.getCreatedTime()).isNotNull();
    }

    @Test
    void create_throwsIllegalStateException_whenSerializationFails() {
        String aggregateId = UUID.randomUUID().toString();

        assertThatThrownBy(() -> factory.create(aggregateId, "Unserializable", new Unserializable()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unserializable")
                .hasMessageContaining(aggregateId);
    }
}
