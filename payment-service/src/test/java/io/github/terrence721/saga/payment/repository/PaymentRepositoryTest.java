package io.github.terrence721.saga.payment.repository;

import io.github.terrence721.saga.payment.domain.Payment;
import io.github.terrence721.saga.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void findByOrderId_returnsPayment_whenOneExists() {
        UUID orderId = UUID.randomUUID();
        Payment newPayment = Payment.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("19.99"))
                .status(PaymentStatus.APPROVED)
                .build();
        paymentRepository.save(newPayment);

        Optional<Payment> found = paymentRepository.findByOrderId(orderId);

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void findByOrderId_returnsEmpty_whenNoneExists() {
        Optional<Payment> found = paymentRepository.findByOrderId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByOrderId_returnsTrue_whenPaymentExists() {
        UUID orderId = UUID.randomUUID();
        Payment newPayment = Payment.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("19.99"))
                .status(PaymentStatus.APPROVED)
                .build();
        paymentRepository.save(newPayment);

        assertThat(paymentRepository.existsByOrderId(orderId)).isTrue();
    }

    @Test
    void existsByOrderId_returnsFalse_whenNoneExists() {
        assertThat(paymentRepository.existsByOrderId(UUID.randomUUID())).isFalse();
    }

    @Test
    void existsByOrderIdAndStatus_returnsTrue_whenStatusMatches() {
        UUID orderId = UUID.randomUUID();
        Payment newPayment = Payment.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("19.99"))
                .status(PaymentStatus.REFUNDED)
                .build();
        paymentRepository.save(newPayment);

        assertThat(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).isTrue();
    }

    @Test
    void existsByOrderIdAndStatus_returnsFalse_whenStatusDoesNotMatch() {
        UUID orderId = UUID.randomUUID();
        Payment newPayment = Payment.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("19.99"))
                .status(PaymentStatus.APPROVED)
                .build();
        paymentRepository.save(newPayment);

        assertThat(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.REFUNDED)).isFalse();
    }

    @Test
    void existsByOrderIdAndStatus_returnsFalse_whenOrderIdDoesNotExist() {
        assertThat(paymentRepository.existsByOrderIdAndStatus(UUID.randomUUID(), PaymentStatus.REFUNDED)).isFalse();
    }
}
