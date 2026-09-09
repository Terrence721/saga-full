package io.github.terrence721.saga.payment.service;

import io.github.terrence721.saga.payment.domain.OutboxRecord;
import io.github.terrence721.saga.payment.domain.Payment;
import io.github.terrence721.saga.payment.domain.PaymentStatus;
import io.github.terrence721.saga.payment.dto.OrderCreatedEvent;
import io.github.terrence721.saga.payment.dto.PaymentProcessedEvent;
import io.github.terrence721.saga.payment.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.payment.exception.PaymentNotFoundException;
import io.github.terrence721.saga.payment.repository.OutboxRepository;
import io.github.terrence721.saga.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final OutboxRecordFactory outboxRecordFactory;
    private final BigDecimal maxAmount;

    public PaymentService(
            PaymentRepository paymentRepository,
            OutboxRepository outboxRepository,
            OutboxRecordFactory outboxRecordFactory,
            @Value("${app.payment.max-amount:500.00}") BigDecimal maxAmount) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.outboxRecordFactory = outboxRecordFactory;
        this.maxAmount = maxAmount;
    }

    @SuppressWarnings("null") // orderId() is always a real, non-null UUID from a real event.
    @Transactional
    public void processPaymentSaga(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.debug("Payment already exists for order {}; skipping duplicate event", event.orderId());
            return;
        }

        PaymentStatus status = event.totalAmount().compareTo(maxAmount) > 0
                ? PaymentStatus.FAILED
                : PaymentStatus.APPROVED;

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .status(status)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        if (status == PaymentStatus.APPROVED) {
            log.info("Payment {} approved for order {}", savedPayment.getId(), event.orderId());
        } else {
            log.info("Payment {} declined for order {}: amount {} exceeds limit {}",
                    savedPayment.getId(), event.orderId(), event.totalAmount(), maxAmount);
        }

        outboxRepository.save(buildOutboxRecord(savedPayment, event));
    }

    @SuppressWarnings("null") // orderId() is always a real, non-null UUID from a real event.
    @Transactional
    public void handleOrderCompensation(RestaurantRejectedEvent event) {
        if (paymentRepository.existsByOrderIdAndStatus(event.orderId(), PaymentStatus.REFUNDED)) {
            log.debug("Payment for order {} already REFUNDED; skipping duplicate compensation", event.orderId());
            return;
        }

        Payment payment = paymentRepository.findByOrderId(event.orderId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + event.orderId()));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.debug("Payment for order {} already REFUNDED after load; skipping", event.orderId());
            return;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.debug("Payment for order {} already FAILED; nothing was captured, skipping compensation", event.orderId());
            return;
        }
        validateCustomerMatches(payment, event.customerId());

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        log.info("Payment for order {} refunded: {}", event.orderId(), event.reason());
    }

    // customerId crosses the wire on the inbound event but this instance already knows the real
    // value from the payment it just loaded - cross-checking it here catches a corrupted or
    // mismatched message on the shared Kafka topic, the same failure mode order-service's own
    // RestaurantRejectedEvent handling (OrderService.cancelOrder) already guards against.
    private void validateCustomerMatches(Payment payment, UUID eventCustomerId) {
        if (!payment.getCustomerId().equals(eventCustomerId)) {
            throw new IllegalArgumentException("customerId mismatch for order " + payment.getOrderId()
                    + ": payment has " + payment.getCustomerId() + ", event has " + eventCustomerId);
        }
    }

    private OutboxRecord buildOutboxRecord(Payment payment, OrderCreatedEvent event) {
        PaymentProcessedEvent outboxEvent = new PaymentProcessedEvent(
                payment.getOrderId(),
                event.customerId(),
                event.itemCode(),
                event.quantity(),
                payment.getAmount(),
                payment.getStatus()
        );
        return outboxRecordFactory.create(payment.getOrderId().toString(), "PaymentProcessedEvent", outboxEvent);
    }
}
