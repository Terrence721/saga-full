package com.example.saga.restaurant.service;

import com.example.saga.restaurant.domain.InventoryItem;
import com.example.saga.restaurant.domain.InventoryStatus;
import com.example.saga.restaurant.repository.InventoryItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
class RestaurantInventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    private RestaurantInventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new RestaurantInventoryService(inventoryItemRepository);
    }

    @Test
    void verifyAndDeductStock_returnsAllocatedAndDeducts_whenEnoughStock() {
        InventoryItem item = InventoryItem.builder().itemCode("MARGHERITA").stockCount(10).build();
        when(inventoryItemRepository.findByItemCodeForUpdate("MARGHERITA")).thenReturn(Optional.of(item));

        InventoryStatus status = inventoryService.verifyAndDeductStock("MARGHERITA", 3);

        assertThat(status).isEqualTo(InventoryStatus.ALLOCATED);
        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemRepository).save(captor.capture());
        assertThat(captor.getValue().getStockCount()).isEqualTo(7);
    }

    @Test
    void verifyAndDeductStock_returnsInsufficientStock_whenNotEnough() {
        InventoryItem item = InventoryItem.builder().itemCode("MARGHERITA").stockCount(2).build();
        when(inventoryItemRepository.findByItemCodeForUpdate("MARGHERITA")).thenReturn(Optional.of(item));

        InventoryStatus status = inventoryService.verifyAndDeductStock("MARGHERITA", 3);

        assertThat(status).isEqualTo(InventoryStatus.INSUFFICIENT_STOCK);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void verifyAndDeductStock_returnsItemNotFound_whenItemCodeUnknown() {
        when(inventoryItemRepository.findByItemCodeForUpdate("UNKNOWN")).thenReturn(Optional.empty());

        InventoryStatus status = inventoryService.verifyAndDeductStock("UNKNOWN", 1);

        assertThat(status).isEqualTo(InventoryStatus.ITEM_NOT_FOUND);
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void verifyAndDeductStock_returnsItemNotFound_whenItemCodeBlank() {
        InventoryStatus status = inventoryService.verifyAndDeductStock("   ", 1);

        assertThat(status).isEqualTo(InventoryStatus.ITEM_NOT_FOUND);
    }

    @Test
    void verifyAndDeductStock_returnsItemNotFound_whenItemCodeNull() {
        InventoryStatus status = inventoryService.verifyAndDeductStock(null, 1);

        assertThat(status).isEqualTo(InventoryStatus.ITEM_NOT_FOUND);
    }


}
