package com.example.saga.reservation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ReservationServiceApplicationMainTest {

    @Test
    void mainRunsApplication() {
        assertThatCode(() -> ReservationServiceApplication.main(new String[] {
                "--server.port=0"
        })).doesNotThrowAnyException();
    }
}
