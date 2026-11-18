package dev.rental.photorent.repository;

import dev.rental.photorent.model.CustomerUser;
import dev.rental.photorent.model.User;
import dev.rental.photorent.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRole(UserRole role);

    @Query("select c from CustomerUser c where c.id = :id")
    Optional<CustomerUser> findCustomerById(@Param("id") Long id);
}

