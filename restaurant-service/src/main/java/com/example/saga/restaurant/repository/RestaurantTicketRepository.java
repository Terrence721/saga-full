package com.example.saga.restaurant.repository;

import com.example.saga.restaurant.domain.RestaurantTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RestaurantTicketRepository extends JpaRepository<RestaurantTicket, UUID> {

    boolean existsByOrderId(UUID orderId);
}
