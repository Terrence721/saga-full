package com.example.saga.restaurant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class RestaurantServiceApplicationConstructorTest {

    @Test
    void applicationCanBeInstantiated() {
        assertThatCode(() -> new RestaurantServiceApplication()).doesNotThrowAnyException();
    }
}
