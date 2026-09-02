package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class InventoryItemRepositoryTest {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void findById_returnsItem_whenItExists() {
        InventoryItem item = InventoryItem.builder()
                .itemCode("PIZZA_01")
                .stockCount(10)
                .build();
        inventoryItemRepository.save(item);

        Optional<InventoryItem> found = inventoryItemRepository.findById("PIZZA_01");

        assertThat(found).isPresent();
        assertThat(found.get().getStockCount()).isEqualTo(10);
    }

    @Test
    void findById_returnsEmpty_whenItemCodeUnknown() {
        Optional<InventoryItem> found = inventoryItemRepository.findById("UNKNOWN");

        assertThat(found).isEmpty();
    }
}
