package dev.rental.photorent.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("ADMIN")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class AdminUser extends User {

    public AdminUser(String username, String email, String passwordHash) {
        super(username, email, passwordHash, UserRole.ADMIN);
    }

    @Override
    public String getPermissionsDescription() {
        return "Full access: fully-manage equipment catalog, categories, pricing, users";
    }
}
