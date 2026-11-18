package dev.rental.photorent.service;

import dev.rental.photorent.model.AdminUser;
import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.ManagerUser;
import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User register(String username, String email, String passwordHash, UserRole role) {
        if (username == null || username.isBlank() || email == null || email.isBlank()
                || passwordHash == null || passwordHash.isBlank() || role == null) {
            throw new IllegalArgumentException("Username, email, password hash and role are required");
        }
        String cleanUsername = username.trim();
        String cleanEmail = email.trim();

        if (userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new IllegalStateException("Username already taken: " + cleanUsername);
        }
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new IllegalStateException("Email already registered: " + cleanEmail);
        }

        User user = switch (role) {
            case ADMIN -> new AdminUser(cleanUsername, cleanEmail, passwordHash);
            case MANAGER -> new ManagerUser(cleanUsername, cleanEmail, passwordHash);
            case CUSTOMER -> new CustomerUser(cleanUsername, cleanEmail, passwordHash);
        };
        return userRepository.save(user);
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    @Transactional
    public boolean removeById(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}

