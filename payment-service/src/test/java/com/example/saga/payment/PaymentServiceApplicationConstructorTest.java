package com.example.saga.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class PaymentServiceApplicationConstructorTest {

    @Test
    void applicationCanBeInstantiated() {
        assertThatCode(() -> new PaymentServiceApplication()).doesNotThrowAnyException();
    }
}
