package com.example.saga.reservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRecordFactoryTest {

    @Test
    void create_throwsWhenEventCannotBeSerialized() {
        ObjectMapper objectMapper = new ObjectMapper();
        OutboxRecordFactory factory = new OutboxRecordFactory(objectMapper);

        Map<String, Object> selfReferential = new HashMap<>();
        selfReferential.put("self", selfReferential);

        assertThatThrownBy(() -> factory.create("agg-1", "SomeEvent", selfReferential))
                .isInstanceOf(IllegalStateException.class);
    }
}
