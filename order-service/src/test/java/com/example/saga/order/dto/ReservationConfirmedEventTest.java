package com.example.saga.order.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationConfirmedEventTest {

    @Test
    void constructorAndAccessors_roundTrip() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        ReservationConfirmedEvent event = new ReservationConfirmedEvent(orderId, customerId, "T-1", 4);

        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.tableNumber()).isEqualTo("T-1");
        assertThat(event.partySize()).isEqualTo(4);
    }

    @Test
    void equalityAndHashcode_areValueBased() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        ReservationConfirmedEvent a = new ReservationConfirmedEvent(orderId, customerId, "T-1", 4);
        ReservationConfirmedEvent b = new ReservationConfirmedEvent(orderId, customerId, "T-1", 4);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_containsAllComponents() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        ReservationConfirmedEvent event = new ReservationConfirmedEvent(orderId, customerId, "T-1", 4);

        assertThat(event.toString()).contains(orderId.toString(), customerId.toString(), "T-1", "4");
    }
}
