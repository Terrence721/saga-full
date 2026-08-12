package io.github.terrence721.saga.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processPaymentSaga(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderId(event.orderId())) {
            log.debug("Payment already exists for order {}; skipping duplicate event", event.orderId());
            return;
        }

        Payment payment = Payment.builder()
                .orderId(event.orderId())
                .amount(event.totalAmount())
                .status(PaymentStatus.APPROVED)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} approved for order {}", savedPayment.getId(), event.orderId());

        outboxRepository.save(buildOutboxRecord(savedPayment, event));
    }

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

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        log.info("Payment for order {} refunded: {}", event.orderId(), event.reason());
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

        String payload;
        try {
            payload = objectMapper.writeValueAsString(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize PaymentProcessedEvent for order " + payment.getOrderId(), e);
        }

        return OutboxRecord.builder()
                .aggregateId(payment.getOrderId().toString())
                .eventType("PaymentProcessedEvent")
                .payload(payload)
                .createdTime(LocalDateTime.now())
                .build();
    }
}
