package dev.rental.photorent.service;

import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.repository.UserRepository;
import dev.rental.photorent.repository.InMemoryUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);
    }

    @Test
    void shouldRegisterUser() {
        User customer = new CustomerUser(1L, "john_doe", "john@example.com");

        User registered = userService.register(customer);

        assertThat(registered.getUsername()).isEqualTo("john_doe");
        assertThat(userService.getById(1L)).isPresent();
    }

    @Test
    void shouldRejectNullUser() {
        assertThatThrownBy(() -> userService.register(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDuplicateUsername() {
        userService.register(new CustomerUser(1L, "john_doe", "john@example.com"));

        assertThatThrownBy(() -> userService.register(new AdminUser(2L, "john_doe", "admin@example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldGetUsersByRole() {
        userService.register(new AdminUser(1L, "admin1", "admin1@example.com"));
        userService.register(new CustomerUser(2L, "customer1", "customer1@example.com"));

        List<User> admins = userService.getByRole(UserRole.ADMIN);

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getUsername()).isEqualTo("admin1");
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        Optional<User> found = userService.getById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void shouldRemoveUserById() {
        userService.register(new CustomerUser(1L, "john_doe", "john@example.com"));

        boolean removed = userService.removeById(1L);

        assertThat(removed).isTrue();
        assertThat(userService.getById(1L)).isEmpty();
    }
}

