package io.github.terrence721.saga.payment.repository;

import io.github.terrence721.saga.payment.domain.OutboxRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxRepositoryTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void findByOrderByCreatedTimeAsc_returnsEmpty_whenNoRecords() {
        List<OutboxRecord> batch = outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10));

        assertThat(batch).isEmpty();
    }

    @Test
    void findByOrderByCreatedTimeAsc_returnsOldestFirst_upToPageSize() {
        LocalDateTime base = LocalDateTime.now().minusMinutes(2);

        for (int i = 0; i < 12; i++) {
            outboxRepository.save(OutboxRecord.builder()
                    .aggregateId("order-" + i)
                    .eventType("PaymentProcessed")
                    .payload("{\"index\":" + i + "}")
                    .createdTime(base.plusSeconds(i))
                    .build());
        }

        List<OutboxRecord> batch = outboxRepository.findByOrderByCreatedTimeAsc(PageRequest.of(0, 10));

        assertThat(batch).hasSize(10);
        assertThat(batch).extracting(OutboxRecord::getAggregateId)
                .containsExactly(
                        "order-0", "order-1", "order-2", "order-3", "order-4",
                        "order-5", "order-6", "order-7", "order-8", "order-9");
    }
}
