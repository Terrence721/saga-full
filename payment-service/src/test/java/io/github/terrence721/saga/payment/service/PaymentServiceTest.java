package io.github.terrence721.saga.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.terrence721.saga.payment.domain.OutboxRecord;
import io.github.terrence721.saga.payment.domain.Payment;
import io.github.terrence721.saga.payment.domain.PaymentStatus;
import io.github.terrence721.saga.payment.dto.OrderCreatedEvent;
import io.github.terrence721.saga.payment.dto.OrderStatus;
import io.github.terrence721.saga.payment.dto.PaymentProcessedEvent;
import io.github.terrence721.saga.payment.dto.RestaurantRejectedEvent;
import io.github.terrence721.saga.payment.exception.PaymentNotFoundException;
import io.github.terrence721.saga.payment.repository.OutboxRepository;
import io.github.terrence721.saga.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private PaymentService paymentService;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("500.00");

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, outboxRepository, new ObjectMapper(), MAX_AMOUNT);
    }

    private Payment approvedPayment(UUID orderId) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .amount(new BigDecimal("19.99"))
                .status(PaymentStatus.APPROVED)
                .build();
    }

    @Test
    void processPaymentSaga_savesPaymentAndStagesOutboxWithRealSerializedPayload() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, customerId, "ITEM-1", 2, new BigDecimal("19.99"), OrderStatus.PENDING);

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        paymentService.processPaymentSaga(event);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxRecord outboxRecord = outboxCaptor.getValue();
        assertThat(outboxRecord.getAggregateId()).isEqualTo(savedPayment.getOrderId().toString());
        assertThat(outboxRecord.getEventType()).isEqualTo("PaymentProcessedEvent");

        PaymentProcessedEvent deserialized = new ObjectMapper().readValue(outboxRecord.getPayload(), PaymentProcessedEvent.class);
        assertThat(deserialized.orderId()).isEqualTo(orderId);
        assertThat(deserialized.customerId()).isEqualTo(customerId);
        assertThat(deserialized.amount()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(deserialized.status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void processPaymentSaga_declinesPayment_whenAmountExceedsMaxAmount() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, customerId, "ITEM-1", 2, new BigDecimal("500.01"), OrderStatus.PENDING);

        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        paymentService.processPaymentSaga(event);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);

        ArgumentCaptor<OutboxRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxRecord.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        PaymentProcessedEvent deserialized = new ObjectMapper()
                .readValue(outboxCaptor.getValue().getPayload(), PaymentProcessedEvent.class);
        assertThat(deserialized.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void processPaymentSaga_skipsDuplicate_whenPaymentAlreadyExists() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        paymentService.processPaymentSaga(new OrderCreatedEvent(
                orderId, UUID.randomUUID(), "ITEM-1", 2, new BigDecimal("19.99"), OrderStatus.PENDING));

        verify(paymentRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void handleOrderCompensation_shortCircuits_whenAlreadyRefunded() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).thenReturn(true);

        paymentService.handleOrderCompensation(new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Out of stock"));

        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleOrderCompensation_transitionsToRefunded_whenApproved() {
        UUID orderId = UUID.randomUUID();
        Payment existing = approvedPayment(orderId);
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        paymentService.handleOrderCompensation(new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Out of stock"));

        assertThat(existing.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(existing);
    }

    @Test
    void handleOrderCompensation_shortCircuits_whenAlreadyRefundedAfterLoad() {
        UUID orderId = UUID.randomUUID();
        Payment alreadyRefunded = approvedPayment(orderId);
        alreadyRefunded.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(alreadyRefunded));

        paymentService.handleOrderCompensation(new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Out of stock"));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleOrderCompensation_skipsRefund_whenPaymentAlreadyFailed() {
        UUID orderId = UUID.randomUUID();
        Payment failed = approvedPayment(orderId);
        failed.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(failed));

        paymentService.handleOrderCompensation(new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Payment failed"));

        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleOrderCompensation_throwsPaymentNotFoundException_whenPaymentMissing() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).thenReturn(false);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handleOrderCompensation(
                new RestaurantRejectedEvent(orderId, UUID.randomUUID(), "Out of stock")))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
