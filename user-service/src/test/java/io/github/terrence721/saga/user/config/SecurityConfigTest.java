package io.github.terrence721.saga.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Test
    void encodesAndVerifiesARealPassword() {
        String rawPassword = "correct-horse-battery-staple";

        String hash = passwordEncoder.encode(rawPassword);

        assertThat(hash).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, hash)).isTrue();
    }

    @Test
    void rejectsAWrongPassword() {
        String hash = passwordEncoder.encode("correct-horse-battery-staple");

        assertThat(passwordEncoder.matches("wrong-password", hash)).isFalse();
    }
}
