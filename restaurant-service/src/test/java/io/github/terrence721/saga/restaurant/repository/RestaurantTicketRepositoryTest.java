package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.RestaurantTicket;
import io.github.terrence721.saga.restaurant.domain.RestaurantTicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class RestaurantTicketRepositoryTest {

    @Autowired
    private RestaurantTicketRepository restaurantTicketRepository;

    @Test
    void existsByOrderId_returnsTrue_whenTicketExists() {
        UUID orderId = UUID.randomUUID();
        RestaurantTicket ticket = RestaurantTicket.builder()
                .orderId(orderId)
                .status(RestaurantTicketStatus.PREPARING)
                .build();
        restaurantTicketRepository.save(ticket);

        assertThat(restaurantTicketRepository.existsByOrderId(orderId)).isTrue();
    }

    @Test
    void existsByOrderId_returnsFalse_whenNoTicketExists() {
        assertThat(restaurantTicketRepository.existsByOrderId(UUID.randomUUID())).isFalse();
    }

    @Test
    void save_rejectsDuplicateOrderId() {
        UUID orderId = UUID.randomUUID();
        RestaurantTicket firstTicket = RestaurantTicket.builder()
                .orderId(orderId)
                .status(RestaurantTicketStatus.PREPARING)
                .build();
        restaurantTicketRepository.saveAndFlush(firstTicket);

        RestaurantTicket duplicateTicket = RestaurantTicket.builder()
                .orderId(orderId)
                .status(RestaurantTicketStatus.REJECTED)
                .build();
        assertThrows(DataIntegrityViolationException.class, () ->
                restaurantTicketRepository.saveAndFlush(duplicateTicket));
    }
}
