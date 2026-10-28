package com.example.saga.payment.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationRejectedEventTest {

    @Test
    void constructorAndAccessors_roundTrip() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        ReservationRejectedEvent event = new ReservationRejectedEvent(orderId, customerId, "no table");

        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.reason()).isEqualTo("no table");
    }

    @Test
    void equalityAndHashcode_areValueBased() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        ReservationRejectedEvent a = new ReservationRejectedEvent(orderId, customerId, "no table");
        ReservationRejectedEvent b = new ReservationRejectedEvent(orderId, customerId, "no table");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsAllComponents() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        ReservationRejectedEvent event = new ReservationRejectedEvent(orderId, customerId, "no table");

        assertThat(event.toString()).contains(orderId.toString(), customerId.toString(), "no table");
    }
}
