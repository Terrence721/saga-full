package io.github.terrence721.saga.restaurant;

import io.github.terrence721.saga.restaurant.domain.InventoryItem;
import io.github.terrence721.saga.restaurant.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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
                        "PIZZA_06", "PIZZA_07", "PIZZA_08", "PIZZA_09", "PIZZA_10");
        assertThat(inventoryItemRepository.findById("PIZZA_03"))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(2));
        assertThat(inventoryItemRepository.findById("PIZZA_08"))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(1));
    }

}
