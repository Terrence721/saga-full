package io.github.terrence721.saga.restaurant.service;

import io.github.terrence721.saga.restaurant.domain.InventoryStatus;
import io.github.terrence721.saga.restaurant.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantInventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public RestaurantInventoryService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @SuppressWarnings("null") // itemCode is always a real, non-null String from a real caller.
    @Transactional
    public InventoryStatus verifyAndDeductStock(String itemCode, int quantity) {
        return inventoryItemRepository.findByItemCodeForUpdate(itemCode)
                .map(item -> {
                    if (item.getStockCount() < quantity) {
                        return InventoryStatus.INSUFFICIENT_STOCK;
                    }
                    item.setStockCount(item.getStockCount() - quantity);
                    inventoryItemRepository.save(item);
                    return InventoryStatus.ALLOCATED;
                })
                .orElse(InventoryStatus.ITEM_NOT_FOUND);
    }
}
