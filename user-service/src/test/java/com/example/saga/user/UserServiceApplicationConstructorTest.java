package com.example.saga.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class UserServiceApplicationConstructorTest {

    @Test
    void applicationCanBeInstantiated() {
        assertThatCode(() -> new UserServiceApplication()).doesNotThrowAnyException();
    }
}
