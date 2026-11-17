package dev.rental.photorent.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CUSTOMER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class CustomerUser extends User {

    public CustomerUser(String username, String email, String passwordHash) {
        super(username, email, passwordHash, UserRole.CUSTOMER);
    }

    @Override
    public String getPermissionsDescription() {
        return "Browse catalog, book equipment, view own rental history";
    }
}
