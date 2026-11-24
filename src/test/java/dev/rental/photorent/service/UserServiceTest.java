package dev.rental.photorent.service;

import dev.rental.photorent.TestData;
import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.ManagerUser;
import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = UserService.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateSubclassMatchingRole() {
        User admin = userService.register("admin", "admin@example.com", "hash", UserRole.ADMIN);
        User manager = userService.register("manager", "manager@example.com", "hash", UserRole.MANAGER);
        User customer = userService.register("customer", "customer@example.com", "hash", UserRole.CUSTOMER);

        assertThat(admin).isInstanceOf(AdminUser.class);
        assertThat(manager).isInstanceOf(ManagerUser.class);
        assertThat(customer).isInstanceOf(CustomerUser.class);
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(customer.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void shouldTrimUsernameAndEmail() {
        User user = userService.register("  john  ", " john@example.com ", "hash", UserRole.CUSTOMER);

        assertThat(user.getUsername()).isEqualTo("john");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThatThrownBy(() -> userService.register("  ", "john@example.com", "hash", UserRole.CUSTOMER))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectMissingRole() {
        assertThatThrownBy(() -> userService.register("john", "john@example.com", "hash", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("john")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("john", "john@example.com", "hash", UserRole.CUSTOMER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Username already taken");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("john", "john@example.com", "hash", UserRole.CUSTOMER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email already registered");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldGetUsersByRole() {
        AdminUser admin = TestData.admin(1L);
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(admin));

        assertThat(userService.getByRole(UserRole.ADMIN)).containsExactly(admin);
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserNotFound() {
        assertThat(userService.getById(999L)).isEmpty();
    }

    @Test
    void shouldRemoveExistingUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertThat(userService.removeById(1L)).isTrue();
        verify(userRepository).deleteById(1L);
    }

    @Test
    void shouldNotRemoveMissingUser() {
        assertThat(userService.removeById(999L)).isFalse();
        verify(userRepository, never()).deleteById(any());
    }

    // ---- изменение логина и почты ----

    @Test
    void shouldChangeUsernameAndEmail() {
        CustomerUser user = TestData.customer(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.update(1L, " new_name ", "new@example.com");

        assertThat(user.getUsername()).isEqualTo("new_name");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldAllowUserToKeepOwnUsername() {
        CustomerUser user = TestData.customer(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByUsernameIgnoreCase("customer1")).thenReturn(Optional.of(user));

        userService.update(1L, "customer1", "other@example.com");

        assertThat(user.getEmail()).isEqualTo("other@example.com");
    }

    @Test
    void shouldRejectUsernameTakenByAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestData.customer(1L)));
        when(userRepository.findByUsernameIgnoreCase("customer2")).thenReturn(Optional.of(TestData.customer(2L)));

        assertThatThrownBy(() -> userService.update(1L, "customer2", "x@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void shouldRejectEmailTakenByAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(TestData.customer(1L)));
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(TestData.customer(2L)));

        assertThatThrownBy(() -> userService.update(1L, "customer1", "taken@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void shouldRejectUpdateOfUnknownUser() {
        assertThatThrownBy(() -> userService.update(99L, "name", "x@example.com"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
