package io.github.terrence721.saga.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.grpc.server.port=0",
        "app.jwt.secret=test-only-secret-never-used-outside-this-test"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
