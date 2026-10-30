package com.example.saga.user.repository;

import com.example.saga.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User.UserBuilder sampleUser() {
        return User.builder()
                .email("user@example.com")
                .active(true);
    }

    @Test
    void save_rejectsADuplicateEmail_becauseTheColumnIsGenuinelyUnique() {
        User firstUser = sampleUser().build();
        userRepository.saveAndFlush(firstUser);

        User duplicateEmailUser = sampleUser().build();
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateEmailUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}