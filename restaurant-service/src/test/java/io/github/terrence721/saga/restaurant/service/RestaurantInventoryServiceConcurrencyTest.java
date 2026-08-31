package io.github.terrence721.saga.restaurant.service;

import io.github.terrence721.saga.restaurant.domain.InventoryItem;
import io.github.terrence721.saga.restaurant.domain.InventoryStatus;
import io.github.terrence721.saga.restaurant.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// @SpringBootTest, not @DataJpaTest: needs the real transaction manager and a real
// connection pool so two threads can each hold their own genuine, independent
// transaction against the same row - @DataJpaTest wraps the whole test method in one
// shared transaction, which can't reproduce the race this is testing for.
@SpringBootTest
class RestaurantInventoryServiceConcurrencyTest {

    @Autowired
    private RestaurantInventoryService inventoryService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void verifyAndDeductStock_serializesConcurrentRequestsForTheSameItem_soStockNeverOversells() throws Exception {
        String itemCode = "CONCURRENCY_TEST_ITEM";
        inventoryItemRepository.saveAndFlush(InventoryItem.builder().itemCode(itemCode).stockCount(5).build());

        // Only enough stock for one request of 5 - if the two concurrent calls both
        // read the same pre-deduction count (the bug this guards against), both would
        // wrongly return ALLOCATED and the row would go to -5, not 0.
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<InventoryStatus> attempt = () -> {
            readyLatch.countDown();
            startLatch.await();
            return inventoryService.verifyAndDeductStock(itemCode, 5);
        };

        Future<InventoryStatus> first = executor.submit(attempt);
        Future<InventoryStatus> second = executor.submit(attempt);
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        List<InventoryStatus> results = List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertThat(results).containsExactlyInAnyOrder(InventoryStatus.ALLOCATED, InventoryStatus.INSUFFICIENT_STOCK);
        assertThat(inventoryItemRepository.findById(itemCode))
                .hasValueSatisfying(item -> assertThat(item.getStockCount()).isEqualTo(0));
    }
}
