package dev.rental.photorent.repository;

import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User add(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    List<User> findByRole(UserRole role);

    List<User> findAll();

    boolean deleteById(Long id);
}

