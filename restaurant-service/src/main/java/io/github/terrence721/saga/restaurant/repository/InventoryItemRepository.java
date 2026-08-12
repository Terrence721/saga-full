package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
}
