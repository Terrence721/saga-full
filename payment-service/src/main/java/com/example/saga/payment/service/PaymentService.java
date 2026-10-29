package com.example.saga.payment.service;

import com.example.saga.payment.domain.OutboxRecord;
import com.example.saga.payment.domain.Payment;
import com.example.saga.payment.domain.PaymentMethod;
import com.example.saga.payment.domain.PaymentStatus;
import com.example.saga.payment.dto.OrderCreatedEvent;
import com.example.saga.payment.dto.PaymentProcessedEvent;
import com.example.saga.payment.dto.RestaurantRejectedEvent;
import com.example.saga.payment.dto.ReservationRejectedEvent;
import com.example.saga.payment.exception.PaymentNotFoundException;
import com.example.saga.payment.repository.OutboxRepository;
import com.example.saga.payment.repository.PaymentRepository;
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
        validate(event);
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.debug("Payment already exists for order {}; skipping duplicate event", event.orderId());
            return;
        }

        PaymentMethod paymentMethod = event.paymentMethod() == null
                ? PaymentMethod.CASH
                : event.paymentMethod();

        PaymentStatus status;
        if (paymentMethod == PaymentMethod.CASH) {
            status = PaymentStatus.APPROVED;
        } else {
            status = event.totalAmount().compareTo(maxAmount) > 0
                    ? PaymentStatus.FAILED
                    : PaymentStatus.APPROVED;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .customerId(event.customerId())
                .amount(event.totalAmount())
                .paymentMethod(paymentMethod)
                .cardBrand(event.cardBrand())
                .cardLast4(event.cardLast4())
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

    @Transactional
    public void handleOrderCompensation(RestaurantRejectedEvent event) {
        handleCompensation(event.orderId(), event.customerId(), event.reason());
    }

    @Transactional
    public void handleReservationCompensation(ReservationRejectedEvent event) {
        handleCompensation(event.orderId(), event.customerId(), event.reason());
    }

    private void validate(OrderCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        if (event.orderId() == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (event.customerId() == null) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (event.totalAmount() == null || event.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount must be > 0");
        }
        if (event.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
    }

    private void handleCompensation(UUID orderId, UUID customerId, String reason) {
        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)) {
            log.debug("Payment for order {} already REFUNDED; skipping duplicate compensation", orderId);
            return;
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.debug("Payment for order {} already REFUNDED after load; skipping", orderId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.debug("Payment for order {} already FAILED; nothing was captured, skipping compensation", orderId);
            return;
        }
        validateCustomerMatches(payment, customerId);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        log.info("Payment for order {} refunded: {}", orderId, reason);
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
