package com.example.saga.restaurant.service;

import com.example.saga.restaurant.domain.InventoryStatus;
import com.example.saga.restaurant.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantInventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public RestaurantInventoryService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional
    public InventoryStatus verifyAndDeductStock(String itemCode, int quantity) {
        if (itemCode == null || itemCode.isBlank()) {
            return InventoryStatus.ITEM_NOT_FOUND;
        }

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
