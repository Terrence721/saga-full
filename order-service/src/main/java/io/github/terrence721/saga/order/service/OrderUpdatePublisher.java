package io.github.terrence721.saga.order.service;

import io.github.terrence721.saga.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

// Live order-status push, kept independent of OrderService's business rules: a change
// to how orders are validated/transitioned has no reason to touch this, and a change to
// the live-update transport (e.g. swapping Reactor Sinks for a message-broker fan-out)
// has no reason to touch order lifecycle logic either.
@Component
@Slf4j
public class OrderUpdatePublisher {

    // Multicast, not a per-order-id map: v1 is a one-terminal cashier flow, not many
    // concurrent orders, so one shared bus filtered per-subscriber by order ID is simpler
    // and needs no eviction logic. directBestEffort(), not onBackpressureBuffer(): a
    // backpressure buffer queues emissions made while zero subscribers are connected and
    // replays the whole backlog to the first subscriber that arrives - verified for real
    // against the running stack that this produces a genuinely wrong client-visible symptom
    // (a client connecting after an order already reached SUCCESS saw its stream replay a
    // stale PENDING before SUCCESS again, even though the connection's own current-state
    // snapshot already correctly showed SUCCESS). directBestEffort() delivers only to
    // subscribers connected at emission time and drops the value otherwise - the correct
    // semantics here, since streamUpdates() callers already prepend current state.
    private final Sinks.Many<Order> orderUpdates = Sinks.many().multicast().directBestEffort();

    public void publish(Order order) {
        Sinks.EmitResult result = orderUpdates.tryEmitNext(order);
        if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            // Expected, common outcome with directBestEffort(): most orders won't have an
            // active /stream client at the exact moment their status changes - the client's
            // own connection always gets current state from its snapshot regardless.
            log.debug("No active subscriber for order {} update; skipping live push", order.getId());
        } else if (result.isFailure()) {
            log.warn("Failed to emit order update for order {}: {}", order.getId(), result);
        }
    }

    // Live-only: no current-state snapshot. Callers building a client-facing stream (see
    // OrderController's /stream endpoint) are expected to prepend a current-state lookup
    // themselves, so a subscriber always gets current state immediately, then live pushes.
    public Flux<Order> streamUpdates(UUID orderId) {
        return orderUpdates.asFlux().filter(order -> order.getId().equals(orderId));
    }
}
