package com.example.saga.reservation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ReservationServiceApplicationConstructorTest {

    @Test
    void applicationCanBeInstantiated() {
        assertThatCode(() -> new ReservationServiceApplication()).doesNotThrowAnyException();
    }
}
