package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    // Plain findById issues no lock, so two concurrent deductions against the same
    // itemCode (a real occurrence once restaurant-service scales past one instance -
    // the Kafka message key here is orderId, not itemCode, so two different orders
    // for the same item can land on different partitions/instances) could both read
    // the same stockCount before either writes back, then both save() a decrement -
    // a classic lost update that oversells stock. This blocks a second transaction
    // reading the same row until the first commits, so it always sees the real,
    // post-deduction count.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.itemCode = :itemCode")
    Optional<InventoryItem> findByItemCodeForUpdate(String itemCode);
}
