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
                        "MARGHERITA", "PEPPERONI", "HEAT_WAVE", "QUATTRO_FORMAGGI", "VEGGIE",
                        "CAPRICCIOSA", "DIAVOLA", "TRUFFLE", "HAWAIIAN", "SPINACH_RICOTTA",
                        "COLA", "LEMONADE", "GARLIC_BREAD");
        assertThat(inventoryItemRepository.findById("HEAT_WAVE"))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(2));
        assertThat(inventoryItemRepository.findById("TRUFFLE"))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(1));
    }

}
