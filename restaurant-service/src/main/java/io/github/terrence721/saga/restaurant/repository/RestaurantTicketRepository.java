package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.RestaurantTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RestaurantTicketRepository extends JpaRepository<RestaurantTicket, UUID> {

    boolean existsByOrderId(UUID orderId);
}
