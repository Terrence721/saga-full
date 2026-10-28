package com.example.saga.restaurant;

import com.example.saga.restaurant.domain.InventoryItem;
import com.example.saga.restaurant.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class RestaurantServiceApplicationTests {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void dataSqlSeedsInventory_onARealBoot() {
        List<InventoryItem> seeded = inventoryItemRepository.findAll();

        assertThat(seeded).extracting(InventoryItem::getItemCode)
                .containsExactlyInAnyOrder(
                        "PIZZA_01", "PIZZA_02", "PIZZA_03", "PIZZA_04", "PIZZA_05",
                        "PIZZA_06", "PIZZA_07", "PIZZA_08", "PIZZA_09", "PIZZA_10",
                        "DRINK_01", "DRINK_02", "SIDE_01");
        assertThat(inventoryItemRepository.findById("PIZZA_03"))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(2));
        assertThat(inventoryItemRepository.findById("PIZZA_08"))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(1));
    }

}
