package com.example.saga.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class UserServiceApplicationMainTest {

    @Test
    void mainRunsApplication() {
        assertThatCode(() -> UserServiceApplication.main(new String[] {
                "--spring.grpc.server.port=0",
                "--app.jwt.secret=test-only-secret-never-used-outside-this-test",
                "--app.jwt.issuer=saga-test-issuer"
        })).doesNotThrowAnyException();
    }
}
