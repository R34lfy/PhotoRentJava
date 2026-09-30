package dev.rental.photorent.repository;

import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.CustomerUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserRepositoryTest {

    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    void shouldAddAndFindUserById() {
        User customer = new CustomerUser(1L, "john_doe", "john@example.com");

        repository.add(customer);
        Optional<User> found = repository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("john_doe");
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        Optional<User> found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindUserByUsernameIgnoringCase() {
        repository.add(new CustomerUser(1L, "john_doe", "john@example.com"));

        Optional<User> found = repository.findByUsername("JOHN_DOE");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
    }

    @Test
    void shouldFindUsersByRole() {
        repository.add(new AdminUser(1L, "admin1", "admin1@example.com"));
        repository.add(new CustomerUser(2L, "customer1", "customer1@example.com"));

        List<User> admins = repository.findByRole(UserRole.ADMIN);

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getUsername()).isEqualTo("admin1");
    }

    @Test
    void shouldDeleteUserById() {
        repository.add(new CustomerUser(1L, "john_doe", "john@example.com"));

        boolean deleted = repository.deleteById(1L);

        assertThat(deleted).isTrue();
        assertThat(repository.findById(1L)).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentUser() {
        boolean deleted = repository.deleteById(999L);

        assertThat(deleted).isFalse();
    }
}

