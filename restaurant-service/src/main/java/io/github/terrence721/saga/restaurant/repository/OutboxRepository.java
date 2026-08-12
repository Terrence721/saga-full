package io.github.terrence721.saga.restaurant.repository;

import io.github.terrence721.saga.restaurant.domain.OutboxRecord;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxRecord, UUID> {

    // The -2 timeout hint tells Hibernate to append "FOR UPDATE SKIP LOCKED":
    // when multiple restaurant-service instances poll this table concurrently,
    // each one skips rows another instance already has locked instead of
    // blocking on them, so they don't publish the same event twice.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    List<OutboxRecord> findByOrderByCreatedTimeAsc(Pageable pageable);
}
