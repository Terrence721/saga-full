package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.RestaurantTicket;
import io.github.terrence721.saga.restaurant.domain.RestaurantTicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RestaurantTicketRepositoryTest {

    @Autowired
    private RestaurantTicketRepository restaurantTicketRepository;

    @Test
    void existsByOrderId_returnsTrue_whenTicketExists() {
        UUID orderId = UUID.randomUUID();
        restaurantTicketRepository.save(RestaurantTicket.builder()
                .orderId(orderId)
                .status(RestaurantTicketStatus.PREPARING)
                .build());

        assertThat(restaurantTicketRepository.existsByOrderId(orderId)).isTrue();
    }

    @Test
    void existsByOrderId_returnsFalse_whenNoTicketExists() {
        assertThat(restaurantTicketRepository.existsByOrderId(UUID.randomUUID())).isFalse();
    }
}
