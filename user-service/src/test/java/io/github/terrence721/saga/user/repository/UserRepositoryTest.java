package io.github.terrence721.saga.user.repository;

import io.github.terrence721.saga.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User.UserBuilder sampleUser() {
        return User.builder()
                .email("user@example.com")
                .passwordHash("hashed-password")
                .active(true);
    }

    @Test
    void findByEmail_returnsUser_whenEmailExists() {
        User saved = userRepository.save(sampleUser().build());

        assertThat(userRepository.findByEmail("user@example.com"))
                .isPresent()
                .get()
                .extracting(User::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void save_rejectsADuplicateEmail_becauseTheColumnIsGenuinelyUnique() {
        userRepository.saveAndFlush(sampleUser().build());

        assertThatThrownBy(() -> userRepository.saveAndFlush(sampleUser().passwordHash("different-hash").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
