package io.github.terrence721.saga.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.jwt.secret=test-only-secret-never-used-outside-this-test"
})
class ApiGatewayServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
