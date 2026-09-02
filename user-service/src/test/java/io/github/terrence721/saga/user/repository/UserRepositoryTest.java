package io.github.terrence721.saga.user.repository;

import io.github.terrence721.saga.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@SuppressWarnings("null") // test fixtures/mocks here are always real, non-null values.
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
        User newUser = sampleUser().build();
        User saved = userRepository.save(newUser);

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
        User firstUser = sampleUser().build();
        userRepository.saveAndFlush(firstUser);

        User duplicateEmailUser = sampleUser().passwordHash("different-hash").build();
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateEmailUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
