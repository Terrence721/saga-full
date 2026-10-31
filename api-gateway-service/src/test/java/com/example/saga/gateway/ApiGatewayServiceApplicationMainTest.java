package com.example.saga.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-never-used-outside-this-test"
})
class ApiGatewayServiceApplicationMainTest {

    @Test
    void applicationCanBeInstantiated() {
        assertThatCode(() -> new ApiGatewayServiceApplication()).doesNotThrowAnyException();
    }

    @Test
    void mainRunsApplication() {
        assertThatCode(() -> ApiGatewayServiceApplication.main(new String[] {
                "--server.port=0",
                "--app.jwt.secret=test-only-secret-never-used-outside-this-test"
        })).doesNotThrowAnyException();
    }
}
